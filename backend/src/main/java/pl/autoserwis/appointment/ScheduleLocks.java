package pl.autoserwis.appointment;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Lock order: configuration gate, appointment row (if any), then target day. */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class ScheduleLocks {
    private final WorkshopScheduleSettingsRepository settings;

    public ScheduleLocks(WorkshopScheduleSettingsRepository settings) {
        this.settings = settings;
    }

    public void forBooking() {
        settings.lockShared();
    }

    public void forConfiguration() {
        settings.lockExclusive();
    }
}
