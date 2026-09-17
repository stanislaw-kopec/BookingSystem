package pl.autoserwis.appointment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/** Gross prices are authoritative; document totals are sums of rounded line amounts. */
public final class RepairAmounts {
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");
    public static final BigDecimal VAT_RATE = new BigDecimal("0.23");
    private RepairAmounts() {}

    public static BigDecimal lineGross(BigDecimal quantity, BigDecimal unitGross) {
        return quantity.multiply(unitGross).setScale(2, RoundingMode.HALF_UP);
    }

    public static Amounts split(BigDecimal gross) {
        BigDecimal roundedGross = gross.setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal net = roundedGross.divide(BigDecimal.ONE.add(VAT_RATE), 2, RoundingMode.HALF_UP);
        return new Amounts(net, roundedGross.subtract(net), roundedGross);
    }

    public static BigDecimal validatedTotal(List<RepairItemDraft> items) {
        BigDecimal total = new BigDecimal("0.00");
        for (int index = 0; index < items.size(); index++) {
            RepairItemDraft item = items.get(index);
            BigDecimal gross = lineGross(item.quantity(), item.unitGrossAmount());
            if (gross.signum() <= 0 || gross.compareTo(MAX_AMOUNT) > 0) {
                throw new AppointmentValidationException(Map.of("repairItems[" + index + "].unitGrossAmount",
                    "Rounded item gross amount must be between 0.01 and 99999999.99."));
            }
            total = total.add(gross);
        }
        if (total.signum() <= 0 || total.compareTo(MAX_AMOUNT) > 0) {
            throw new AppointmentValidationException(Map.of("repairItems",
                "Total repair gross amount must be between 0.01 and 99999999.99."));
        }
        return total;
    }

    public record Amounts(BigDecimal net, BigDecimal vat, BigDecimal gross) {}
}
