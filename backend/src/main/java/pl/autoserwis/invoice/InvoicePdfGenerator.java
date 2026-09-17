package pl.autoserwis.invoice;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import pl.autoserwis.appointment.RepairItemType;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InvoicePdfGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, BaseFont.CP1250, 20);
    private static final Font HEADING_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, BaseFont.CP1250, 12);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, BaseFont.CP1250, 10);
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, BaseFont.CP1250, 8);

    public InvoiceFile generate(InvoiceSnapshot invoice) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 42, 42, 42, 42);
            PdfWriter.getInstance(document, output);
            document.open();
            addHeader(document, invoice);
            addParties(document, invoice);
            PdfPTable vehicle = new PdfPTable(1);
            vehicle.setWidthPercentage(100);
            vehicle.addCell(section("Pojazd", invoice.vehicleLines()));
            document.add(vehicle);
            document.add(space());
            addItems(document, invoice);
            addPaymentSummary(document, invoice);
            Paragraph footer = new Paragraph("Dokument demonstracyjny systemu Mietek Customs. Dane sprzedawcy są przykładowe.", SMALL_FONT);
            footer.setSpacingBefore(24);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
            document.close();
            return new InvoiceFile(invoice.filename(), output.toByteArray());
        } catch (Exception exception) {
            throw new InvoiceGenerationException("Could not generate invoice PDF.", exception);
        }
    }

    private void addHeader(Document document, InvoiceSnapshot invoice) throws Exception {
        PdfPTable header = new PdfPTable(new float[] { 1, 2 });
        header.setWidthPercentage(100);
        PdfPCell logoCell = cell();
        ClassPathResource logo = new ClassPathResource("branding/mietek-customs-logo.png");
        if (logo.exists()) {
            try (var stream = logo.getInputStream()) {
                Image image = Image.getInstance(stream.readAllBytes());
                image.scaleToFit(120, 80);
                logoCell.addElement(image);
            }
        } else {
            logoCell.addElement(new Phrase(invoice.sellerLines().getFirst(), HEADING_FONT));
        }
        header.addCell(logoCell);
        PdfPCell titleCell = cell(Element.ALIGN_RIGHT);
        titleCell.addElement(aligned("Faktura VAT", TITLE_FONT, Element.ALIGN_RIGHT));
        titleCell.addElement(aligned("Nr " + invoice.number(), HEADING_FONT, Element.ALIGN_RIGHT));
        titleCell.addElement(aligned("Data wystawienia: " + invoice.issuedOn().format(DATE_FORMAT), NORMAL_FONT, Element.ALIGN_RIGHT));
        titleCell.addElement(aligned("Data sprzedaży: " + invoice.soldOn().format(DATE_FORMAT), NORMAL_FONT, Element.ALIGN_RIGHT));
        header.addCell(titleCell);
        document.add(header);
        document.add(space());
    }

    private void addParties(Document document, InvoiceSnapshot invoice) throws DocumentException {
        PdfPTable parties = new PdfPTable(2);
        parties.setWidthPercentage(100);
        parties.setSpacingBefore(8);
        parties.addCell(section("Sprzedawca", invoice.sellerLines()));
        parties.addCell(section("Nabywca", invoice.buyerLines()));
        document.add(parties);
        document.add(space());
    }

    private void addItems(Document document, InvoiceSnapshot invoice) throws DocumentException {
        Paragraph description = new Paragraph("Opis wykonanych prac: " + invoice.repairDescription(), NORMAL_FONT);
        description.setSpacingAfter(10);
        document.add(description);
        PdfPTable table = new PdfPTable(new float[] { 0.45f, 1.1f, 2.15f, 0.85f, 1.1f, 1.1f, 1.1f });
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String label : List.of("Lp.", "Typ", "Pozycja", "Ilość", "Cena brutto", "Wartość netto", "Wartość brutto")) {
            table.addCell(headerCell(label));
        }
        for (InvoiceSnapshot.Item item : invoice.items()) {
            table.addCell(bodyCell(String.valueOf(item.order())));
            table.addCell(bodyCell(item.type() == RepairItemType.PART ? "Część" : "Robocizna"));
            table.addCell(bodyCell(item.name()));
            table.addCell(amountCell(decimal(item.quantity())));
            table.addCell(amountCell(money(item.unitGross())));
            table.addCell(amountCell(money(item.net())));
            table.addCell(amountCell(money(item.gross())));
        }
        document.add(table);
        document.add(space());
    }

    private void addPaymentSummary(Document document, InvoiceSnapshot invoice) throws DocumentException {
        PdfPTable summary = new PdfPTable(new float[] { 1.5f, 1 });
        summary.setWidthPercentage(60);
        summary.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summary.setKeepTogether(true);
        summary.addCell(summaryLabelCell("Razem netto"));
        summary.addCell(summaryValueCell(money(invoice.totalNet())));
        summary.addCell(summaryLabelCell("VAT " + invoice.vatRate().movePointRight(2).stripTrailingZeros().toPlainString() + "%"));
        summary.addCell(summaryValueCell(money(invoice.totalVat())));
        summary.addCell(summaryLabelCell("Razem brutto (" + invoice.currency() + ")"));
        summary.addCell(summaryValueCell(money(invoice.totalGross())));
        document.add(summary);
        Paragraph payment = new Paragraph(invoice.paymentDescription(), NORMAL_FONT);
        payment.setSpacingBefore(12);
        document.add(payment);
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
        PdfPCell cell = borderedCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, BaseFont.CP1250, 10)));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new java.awt.Color(238, 242, 247));
        return cell;
    }

    private PdfPCell bodyCell(String value) {
        PdfPCell cell = borderedCell(new Phrase(value, NORMAL_FONT));
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell amountCell(String value) {
        PdfPCell cell = borderedCell(new Phrase(value, SMALL_FONT));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
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

    private String decimal(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY).toPlainString().replace('.', ',');
    }

    private String money(BigDecimal value) {
        return decimal(value) + " zł";
    }
}
