package pl.autoserwis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.autoserwis.appointment.schedule.AppointmentSchedule;

import java.time.Clock;

@Configuration
public class TimeConfiguration {

    @Bean
    Clock workshopClock() {
        return Clock.system(AppointmentSchedule.TIME_ZONE);
    }
}
