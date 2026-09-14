package pl.autoserwis.invoice;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import pl.autoserwis.appointment.AppointmentRepairItem;
import pl.autoserwis.appointment.AppointmentRequest;
import pl.autoserwis.appointment.RepairItemType;
import pl.autoserwis.appointment.AppointmentSchedule;
import pl.autoserwis.profile.ClientProfile;

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

    public InvoiceFile generate(AppointmentRequest appointment, ClientProfile profile) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 42, 42, 42, 42);
            PdfWriter.getInstance(document, output);
            document.open();

            addHeader(document, appointment);
            addParties(document, profile);
            addVehicle(document, appointment);
            addItems(document, appointment);
            addPaymentSummary(document, appointment);
            addFooter(document);

            document.close();
            return new InvoiceFile(filename(appointment), output.toByteArray());
        } catch (Exception exception) {
            throw new InvoiceGenerationException("Could not generate invoice PDF.", exception);
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

    private void addVehicle(Document document, AppointmentRequest appointment) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.addCell(section("Pojazd", List.of(
            appointment.getVehicleMake() + " " + appointment.getVehicleModel()
                + ", rok " + appointment.getVehicleProductionYear(),
            "Numer rejestracyjny: " + appointment.getVehicleRegistrationNumber(),
            "VIN: " + (appointment.getVehicleVin() == null || appointment.getVehicleVin().isBlank()
                ? "nie podano" : appointment.getVehicleVin()),
            "Numer zgłoszenia: " + appointment.getReference()
        )));
        document.add(table);
        document.add(space());
    }

    private void addItems(Document document, AppointmentRequest appointment) throws DocumentException {
        Paragraph description = new Paragraph("Opis wykonanych prac: " + appointment.getRepairDescription(), NORMAL_FONT);
        description.setSpacingAfter(10);
        document.add(description);

        PdfPTable table = new PdfPTable(new float[] { 0.5f, 0.9f, 2.4f, 0.7f, 1.1f, 1.1f, 1.1f });
        table.setWidthPercentage(100);
        table.addCell(headerCell("Lp."));
        table.addCell(headerCell("Typ"));
        table.addCell(headerCell("Pozycja"));
        table.addCell(headerCell("Ilość"));
        table.addCell(headerCell("Cena netto"));
        table.addCell(headerCell("Wartość netto"));
        table.addCell(headerCell("Wartość brutto"));

        List<AppointmentRepairItem> items = appointment.getRepairItems();
        if (items.isEmpty()) {
            BigDecimal gross = appointment.getTotalGrossAmount();
            table.addCell(bodyCell("1"));
            table.addCell(bodyCell("Usługa"));
            table.addCell(bodyCell(appointment.getRepairDescription()));
            table.addCell(bodyCell("1,00"));
            table.addCell(bodyCell(money(netFromGross(gross))));
            table.addCell(bodyCell(money(netFromGross(gross))));
            table.addCell(bodyCell(money(gross)));
        } else {
            for (AppointmentRepairItem item : items) {
                table.addCell(bodyCell(String.valueOf(item.getItemOrder())));
                table.addCell(bodyCell(itemTypeLabel(item.getType())));
                table.addCell(bodyCell(item.getName()));
                table.addCell(bodyCell(decimal(item.getQuantity())));
                table.addCell(bodyCell(money(netFromGross(item.getUnitGrossAmount()))));
                table.addCell(bodyCell(money(netFromGross(item.getTotalGrossAmount()))));
                table.addCell(bodyCell(money(item.getTotalGrossAmount())));
            }
        }
        document.add(table);
        document.add(space());
    }

    private void addPaymentSummary(Document document, AppointmentRequest appointment) throws DocumentException {
        BigDecimal gross = appointment.getTotalGrossAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = netFromGross(gross);
        BigDecimal vat = gross.subtract(net).setScale(2, RoundingMode.HALF_UP);

        PdfPTable summary = new PdfPTable(new float[] { 2, 1 });
        summary.setWidthPercentage(45);
        summary.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summary.addCell(summaryLabelCell("Razem netto"));
        summary.addCell(summaryValueCell(money(net)));
        summary.addCell(summaryLabelCell("VAT 23%"));
        summary.addCell(summaryValueCell(money(vat)));
        summary.addCell(summaryLabelCell("Razem brutto"));
        summary.addCell(summaryValueCell(money(gross)));
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

    private PdfPCell summaryLabelCell(String value) {
        PdfPCell cell = borderedCell(new Phrase(value, HEADING_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private PdfPCell summaryValueCell(String value) {
        return summaryValueCell(value, HEADING_FONT);
    }

    private PdfPCell summaryValueCell(String value, Font font) {
        PdfPCell cell = borderedCell(new Phrase(value, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
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

    private BigDecimal netFromGross(BigDecimal gross) {
        return gross.divide(BigDecimal.ONE.add(VAT_RATE), 2, RoundingMode.HALF_UP);
    }

    private String itemTypeLabel(RepairItemType type) {
        return type == RepairItemType.PART ? "Część" : "Robocizna";
    }

    private String decimal(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " zł";
    }
}
