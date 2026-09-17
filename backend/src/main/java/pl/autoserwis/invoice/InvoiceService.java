package pl.autoserwis.invoice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.appointment.*;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.profile.ClientProfile;
import pl.autoserwis.profile.ClientProfileRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class InvoiceService {
    private final InvoiceDocumentRepository documents;
    private final AppointmentRepository appointments;
    private final ClientProfileRepository profiles;
    private final InvoicePdfGenerator generator;
    private final Clock clock;

    public InvoiceService(InvoiceDocumentRepository documents, AppointmentRepository appointments,
            ClientProfileRepository profiles, InvoicePdfGenerator generator, Clock invoiceClock) {
        this.documents = documents;
        this.appointments = appointments;
        this.profiles = profiles;
        this.generator = generator;
        this.clock = invoiceClock;
    }

    /** Caller must authorize access before calling. The row lock also serializes legacy first downloads. */
    @Transactional
    public InvoiceFile documentFor(Long appointmentId) {
        return documents.findById(appointmentId).map(InvoiceDocument::file).orElseGet(() -> {
            AppointmentRequest appointment = appointments.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Completed repair not found."));
            return documents.findById(appointmentId).map(InvoiceDocument::file)
                .orElseGet(() -> issue(appointment).file());
        });
    }

    private InvoiceDocument issue(AppointmentRequest appointment) {
        if (appointment.getStatus() != AppointmentStatus.COMPLETED
                || appointment.getRequesterType() != AppointmentRequesterType.CLIENT
                || appointment.getClient() == null || appointment.getVehicle() == null) {
            throw new ResourceNotFoundException("Invoice is available only for a completed repair of a registered client.");
        }
        ClientProfile profile = profiles.findByUser_Id(appointment.getClient().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Client profile not found."));
        InvoiceSnapshot snapshot = snapshot(appointment, profile);
        InvoiceFile pdf = generator.generate(snapshot);
        return documents.save(new InvoiceDocument(appointment.getId(), snapshot, pdf.content()));
    }

    private InvoiceSnapshot snapshot(AppointmentRequest appointment, ClientProfile profile) {
        LocalDate saleDate = appointment.getVehiclePickedUpAt().atZone(AppointmentSchedule.TIME_ZONE).toLocalDate();
        String number = "MC/" + saleDate.getYear() + "/" + String.format(Locale.ROOT, "%06d", appointment.getId());
        List<InvoiceSnapshot.Item> items = new ArrayList<>();
        if (appointment.getRepairItems().isEmpty()) {
            // Repairs created before itemized orders retain their original gross total.
            items.add(item(1, RepairItemType.LABOR, appointment.getRepairDescription(),
                BigDecimal.ONE.setScale(2), appointment.getTotalGrossAmount(), appointment.getTotalGrossAmount()));
        } else {
            for (AppointmentRepairItem source : appointment.getRepairItems()) {
                items.add(item(source.getItemOrder(), source.getType(), source.getName(), source.getQuantity(),
                    source.getUnitGrossAmount(), source.getTotalGrossAmount()));
            }
        }
        BigDecimal net = items.stream().map(InvoiceSnapshot.Item::net).reduce(new BigDecimal("0.00"), BigDecimal::add);
        BigDecimal vat = items.stream().map(InvoiceSnapshot.Item::vat).reduce(new BigDecimal("0.00"), BigDecimal::add);
        BigDecimal gross = items.stream().map(InvoiceSnapshot.Item::gross).reduce(new BigDecimal("0.00"), BigDecimal::add);
        if (gross.compareTo(appointment.getTotalGrossAmount()) != 0) {
            throw new InvoiceGenerationException("Repair item total does not match the repair amount.", null);
        }
        return new InvoiceSnapshot(1, number, LocalDate.now(clock), saleDate, "PLN", RepairAmounts.VAT_RATE,
            List.of("Mietek Customs", "ul. Warsztatowa 7", "50-001 Wrocław", "NIP: 8970000000",
                "kontakt@mietek-customs.example"),
            buyerLines(profile), List.of(
                appointment.getVehicleMake() + " " + appointment.getVehicleModel() + ", rok " + appointment.getVehicleProductionYear(),
                "Numer rejestracyjny: " + appointment.getVehicleRegistrationNumber(),
                "VIN: " + (appointment.getVehicleVin() == null || appointment.getVehicleVin().isBlank() ? "nie podano" : appointment.getVehicleVin()),
                "Numer zgłoszenia: " + appointment.getReference()),
            appointment.getRepairDescription(), items, net, vat, gross,
            "Sposób płatności: płatność na miejscu przy odbiorze auta. Rozliczenie odbywa się poza systemem.");
    }

    private InvoiceSnapshot.Item item(int order, RepairItemType type, String name, BigDecimal quantity,
            BigDecimal unitGross, BigDecimal gross) {
        RepairAmounts.Amounts amounts = RepairAmounts.split(gross);
        return new InvoiceSnapshot.Item(order, type, name, quantity, unitGross, amounts.net(), amounts.vat(), amounts.gross());
    }

    private List<String> buyerLines(ClientProfile profile) {
        if (profile.isHasCompanyData()) {
            return List.of(profile.getCompanyName(), "NIP: " + profile.getTaxId(), profile.getBillingAddressLine(),
                profile.getBillingPostalCode() + " " + profile.getBillingCity());
        }
        return List.of(profile.getFirstName() + " " + profile.getLastName(), profile.getAddressLine(),
            profile.getPostalCode() + " " + profile.getCity(), "E-mail: " + profile.getContactEmail(),
            "Tel.: " + profile.getPhoneNumber());
    }
}
