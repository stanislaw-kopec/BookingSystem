package pl.autoserwis.appointment;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import pl.autoserwis.appointment.dto.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/schedule")
public class AdminScheduleController {
    private final WorkshopScheduleConfigService scheduleConfig;

    public AdminScheduleController(WorkshopScheduleConfigService scheduleConfig) {
        this.scheduleConfig = scheduleConfig;
    }

    @GetMapping
    public WorkshopScheduleConfigResponse getConfig() {
        return scheduleConfig.config();
    }

    @PutMapping("/settings")
    public WorkshopScheduleConfigResponse updateSettings(@Valid @RequestBody ScheduleSettingsRequest request) {
        return scheduleConfig.updateSettings(request);
    }

    @PutMapping("/overrides/{date}")
    public WorkshopScheduleConfigResponse saveOverride(@PathVariable LocalDate date,
            @Valid @RequestBody ScheduleDayOverrideRequest request) {
        ScheduleDayOverrideRequest normalizedRequest = new ScheduleDayOverrideRequest(
            date, request.capacity(), request.closed(), request.note());
        return scheduleConfig.saveOverride(normalizedRequest);
    }

    @DeleteMapping("/overrides/{date}")
    public WorkshopScheduleConfigResponse deleteOverride(@PathVariable LocalDate date) {
        return scheduleConfig.deleteOverride(date);
    }
}
