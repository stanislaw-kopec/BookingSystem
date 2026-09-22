package pl.autoserwis.appointment.repair;

import org.springframework.stereotype.Component;
import pl.autoserwis.appointment.domain.AppointmentValidationException;
import pl.autoserwis.appointment.domain.RepairItemDraft;
import pl.autoserwis.appointment.repair.dto.RepairItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RepairItemValidator {

    public List<RepairItemDraft> normalize(List<RepairItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new AppointmentValidationException(Map.of("repairItems",
                "Add at least one repair item."));
        }
        List<RepairItemDraft> normalized = new ArrayList<>();
        Map<String, String> errors = new LinkedHashMap<>();
        for (int index = 0; index < items.size(); index++) {
            RepairItemRequest item = items.get(index);
            String prefix = "repairItems[" + index + "].";
            if (item == null) {
                errors.put("repairItems[" + index + "]", "Complete the repair item.");
                continue;
            }
            String name = item.name() == null ? "" : item.name().strip();
            if (name.isBlank()) {
                errors.put(prefix + "name", "Enter an item name.");
            }
            if (item.type() == null) {
                errors.put(prefix + "type", "Select an item type.");
            }
            BigDecimal quantity = normalizedDecimal(item.quantity(), prefix + "quantity", errors,
                "Quantity can have at most 2 decimal places.");
            BigDecimal unitGrossAmount = normalizedDecimal(item.unitGrossAmount(), prefix + "unitGrossAmount", errors,
                "Gross price can have at most 2 decimal places.");
            if (item.type() != null && !name.isBlank() && quantity != null && unitGrossAmount != null) {
                normalized.add(new RepairItemDraft(item.type(), name, quantity, unitGrossAmount));
            }
        }
        if (!errors.isEmpty()) {
            throw new AppointmentValidationException(errors);
        }
        return normalized;
    }

    private BigDecimal normalizedDecimal(BigDecimal value, String field,
            Map<String, String> errors, String message) {
        if (value == null) {
            return null;
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            errors.put(field, message);
            return null;
        }
    }
}
