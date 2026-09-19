package pl.autoserwis.appointment;

import org.junit.jupiter.api.Test;
import pl.autoserwis.appointment.dto.RepairItemRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepairItemValidatorTest {
    private final RepairItemValidator validator = new RepairItemValidator();

    @Test
    void normalizesNamesAndDecimalScale() {
        List<RepairItemDraft> result = validator.normalize(List.of(
            new RepairItemRequest(RepairItemType.LABOR, "  Diagnostyka  ",
                new BigDecimal("1"), new BigDecimal("150.5"))));

        assertThat(result).containsExactly(new RepairItemDraft(
            RepairItemType.LABOR, "Diagnostyka", new BigDecimal("1.00"), new BigDecimal("150.50")));
    }

    @Test
    void reportsEveryInvalidFieldInRepairItems() {
        RepairItemRequest invalid = new RepairItemRequest(null, "  ",
            new BigDecimal("1.001"), new BigDecimal("10.999"));

        assertThatThrownBy(() -> validator.normalize(List.of(invalid)))
            .isInstanceOfSatisfying(AppointmentValidationException.class, exception ->
                assertThat(exception.getFieldErrors()).containsOnlyKeys(
                    "repairItems[0].name",
                    "repairItems[0].type",
                    "repairItems[0].quantity",
                    "repairItems[0].unitGrossAmount"));
    }

    @Test
    void rejectsMissingRepairItems() {
        assertThatThrownBy(() -> validator.normalize(List.of()))
            .isInstanceOfSatisfying(AppointmentValidationException.class, exception ->
                assertThat(exception.getFieldErrors()).containsKey("repairItems"));
    }
}
