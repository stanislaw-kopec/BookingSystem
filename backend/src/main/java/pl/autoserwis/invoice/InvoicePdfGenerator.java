package pl.autoserwis.invoice;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import pl.autoserwis.appointment.AppointmentRequest;
import pl.autoserwis.appointment.AppointmentSchedule;
import pl.autoserwis.profile.ClientProfile;
import pl.autoserwis.vehicle.Vehicle;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class InvoicePdfGenerator {
    private static final BigDecimal VAT_RATE = new BigDecimal("0.23");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, BaseFont.CP1250, 20);
    private static final Font HEADING_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, BaseFont.CP1250, 12);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, BaseFont.CP1250, 10);
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, BaseFont.CP1250, 8);

    public InvoiceFile generate(AppointmentRequest appointment, Vehicle vehicle, ClientProfile profile) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 42, 42, 42, 42);
            PdfWriter.getInstance(document, output);
            document.open();

            addHeader(document, appointment);
            addParties(document, profile);
            addVehicle(document, vehicle, appointment);
            addItems(document, appointment);
            addPaymentSummary(document, appointment);
            addFooter(document);

            document.close();
            return new InvoiceFile(filename(appointment), output.toByteArray());
        } catch (Exception exception) {
            throw new InvoiceGenerationException("Nie udało się wygenerować faktury PDF.", exception);
        }
    }

    private void addHeader(Document document, AppointmentRequest appointment) throws Exception {
        PdfPTable header = new PdfPTable(new float[] { 1, 2 });
        header.setWidthPercentage(100);
        PdfPCell logoCell = cell();
        ClassPathResource logo = new ClassPathResource("branding/mietek-customs-logo.png");
        if (logo.exists()) {
            Image image = Image.getInstance(logo.getInputStream().readAllBytes());
            image.scaleToFit(120, 80);
            logoCell.addElement(image);
        } else {
            logoCell.addElement(new Phrase("Mietek Customs", HEADING_FONT));
        }
        header.addCell(logoCell);

        PdfPCell titleCell = cell(Element.ALIGN_RIGHT);
        titleCell.addElement(aligned("Faktura VAT", TITLE_FONT, Element.ALIGN_RIGHT));
        titleCell.addElement(aligned("Nr " + invoiceNumber(appointment), HEADING_FONT, Element.ALIGN_RIGHT));
        titleCell.addElement(aligned("Data wystawienia: " + today(), NORMAL_FONT, Element.ALIGN_RIGHT));
        titleCell.addElement(aligned("Data sprzedaży: " + date(appointment.getVehiclePickedUpAt()), NORMAL_FONT, Element.ALIGN_RIGHT));
        header.addCell(titleCell);
        document.add(header);
        document.add(space());
    }

    private void addParties(Document document, ClientProfile profile) throws DocumentException {
        PdfPTable parties = new PdfPTable(2);
        parties.setWidthPercentage(100);
        parties.setSpacingBefore(8);
        parties.addCell(section("Sprzedawca", List.of(
            "Mietek Customs",
            "ul. Warsztatowa 7",
            "50-001 Wrocław",
            "NIP: 8970000000",
            "kontakt@mietek-customs.example"
        )));
        parties.addCell(section("Nabywca", buyerLines(profile)));
        document.add(parties);
        document.add(space());
    }

    private List<String> buyerLines(ClientProfile profile) {
        if (profile.isHasCompanyData()) {
            return List.of(
                profile.getCompanyName(),
                "NIP: " + profile.getTaxId(),
                profile.getBillingAddressLine(),
                profile.getBillingPostalCode() + " " + profile.getBillingCity()
            );
        }
        return List.of(
            profile.getFirstName() + " " + profile.getLastName(),
            profile.getAddressLine(),
            profile.getPostalCode() + " " + profile.getCity(),
            "E-mail: " + profile.getContactEmail(),
            "Tel.: " + profile.getPhoneNumber()
        );
    }

    private void addVehicle(Document document, Vehicle vehicle, AppointmentRequest appointment) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.addCell(section("Pojazd", List.of(
            vehicle.getMake() + " " + vehicle.getModel() + ", rok " + vehicle.getProductionYear(),
            "Numer rejestracyjny: " + vehicle.getRegistrationNumber(),
            "VIN: " + (vehicle.getVin() == null || vehicle.getVin().isBlank() ? "nie podano" : vehicle.getVin()),
            "Numer zgłoszenia: " + appointment.getReference()
        )));
        document.add(table);
        document.add(space());
    }

    private void addItems(Document document, AppointmentRequest appointment) throws DocumentException {
        BigDecimal gross = appointment.getTotalGrossAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = gross.divide(BigDecimal.ONE.add(VAT_RATE), 2, RoundingMode.HALF_UP);
        BigDecimal vat = gross.subtract(net).setScale(2, RoundingMode.HALF_UP);

        PdfPTable table = new PdfPTable(new float[] { 0.7f, 4.2f, 1.1f, 1.3f, 1.3f, 1.3f });
        table.setWidthPercentage(100);
        table.addCell(headerCell("Lp."));
        table.addCell(headerCell("Opis usługi"));
        table.addCell(headerCell("VAT"));
        table.addCell(headerCell("Netto"));
        table.addCell(headerCell("Kwota VAT"));
        table.addCell(headerCell("Brutto"));
        table.addCell(bodyCell("1"));
        table.addCell(bodyCell(appointment.getRepairDescription()));
        table.addCell(bodyCell("23%"));
        table.addCell(bodyCell(money(net)));
        table.addCell(bodyCell(money(vat)));
        table.addCell(bodyCell(money(gross)));
        document.add(table);
        document.add(space());
    }

    private void addPaymentSummary(Document document, AppointmentRequest appointment) throws DocumentException {
        BigDecimal gross = appointment.getTotalGrossAmount().setScale(2, RoundingMode.HALF_UP);
        Paragraph summary = new Paragraph();
        summary.setAlignment(Element.ALIGN_RIGHT);
        summary.add(new Chunk("Razem brutto: ", HEADING_FONT));
        summary.add(new Chunk(money(gross), TITLE_FONT));
        document.add(summary);
        Paragraph payment = new Paragraph("Sposób płatności: płatność na miejscu przy odbiorze auta. Status: zapłacono przy odbiorze.", NORMAL_FONT);
        payment.setSpacingBefore(12);
        document.add(payment);
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph("Dokument wygenerowany automatycznie przez system Mietek Customs.", SMALL_FONT);
        footer.setSpacingBefore(24);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private PdfPCell section(String title, List<String> lines) {
        PdfPCell cell = borderedCell();
        cell.addElement(new Paragraph(title, HEADING_FONT));
        for (String line : lines) {
            cell.addElement(new Paragraph(line, NORMAL_FONT));
        }
        return cell;
    }

    private PdfPCell headerCell(String value) {
        PdfPCell cell = borderedCell(new Phrase(value, HEADING_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new java.awt.Color(238, 242, 247));
        return cell;
    }

    private PdfPCell bodyCell(String value) {
        return borderedCell(new Phrase(value, NORMAL_FONT));
    }

    private PdfPCell cell() {
        return cell(Element.ALIGN_LEFT);
    }

    private PdfPCell cell(int alignment) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private PdfPCell borderedCell() {
        PdfPCell cell = cell();
        cell.setPadding(10);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new java.awt.Color(214, 219, 229));
        return cell;
    }

    private PdfPCell borderedCell(Phrase phrase) {
        PdfPCell cell = borderedCell();
        cell.setPhrase(phrase);
        return cell;
    }

    private Paragraph aligned(String text, Font font, int alignment) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(alignment);
        return paragraph;
    }

    private Paragraph space() {
        Paragraph paragraph = new Paragraph(" ");
        paragraph.setSpacingAfter(10);
        return paragraph;
    }

    private String invoiceNumber(AppointmentRequest appointment) {
        LocalDate date = appointment.getVehiclePickedUpAt().atZone(AppointmentSchedule.TIME_ZONE).toLocalDate();
        return "MC/" + date.getYear() + "/" + String.format(Locale.ROOT, "%06d", appointment.getId());
    }

    private String filename(AppointmentRequest appointment) {
        return "invoice-" + invoiceNumber(appointment).replace('/', '-') + ".pdf";
    }

    private String today() {
        return LocalDate.now(AppointmentSchedule.TIME_ZONE).format(DATE_FORMAT);
    }

    private String date(java.time.Instant value) {
        return value.atZone(AppointmentSchedule.TIME_ZONE).toLocalDate().format(DATE_FORMAT);
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " zł";
    }
}
