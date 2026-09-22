package pl.autoserwis.appointment.repair;

import org.springframework.stereotype.Component;
import pl.autoserwis.appointment.domain.AppointmentRequest;
import pl.autoserwis.appointment.repair.dto.RepairItemResponse;

import java.util.List;

@Component
public class RepairItemResponseMapper {

    public List<RepairItemResponse> toResponses(AppointmentRequest appointment) {
        return appointment.getRepairItems().stream()
            .map(item -> new RepairItemResponse(item.getId(), item.getType(), item.getName(),
                item.getQuantity(), item.getUnitGrossAmount(), item.getTotalGrossAmount()))
            .toList();
    }
}
