package pl.autoserwis.invoice;

import pl.autoserwis.appointment.RepairItemType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Versioned, immutable business data; never rebuilt from a later customer profile. */
public record InvoiceSnapshot(
    int version, String number, LocalDate issuedOn, LocalDate soldOn, String currency,
    BigDecimal vatRate, List<String> sellerLines, List<String> buyerLines,
    List<String> vehicleLines, String repairDescription, List<Item> items,
    BigDecimal totalNet, BigDecimal totalVat, BigDecimal totalGross, String paymentDescription
) {
    public InvoiceSnapshot {
        sellerLines = List.copyOf(sellerLines);
        buyerLines = List.copyOf(buyerLines);
        vehicleLines = List.copyOf(vehicleLines);
        items = List.copyOf(items);
    }

    public record Item(int order, RepairItemType type, String name, BigDecimal quantity,
                       BigDecimal unitGross, BigDecimal net, BigDecimal vat, BigDecimal gross) {}

    public String filename() {
        return "invoice-" + number.replace('/', '-') + ".pdf";
    }
}
