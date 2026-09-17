package pl.autoserwis.invoice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.autoserwis.appointment.AppointmentSchedule;
import java.time.Clock;

@Configuration
public class InvoiceConfiguration {
    @Bean
    Clock invoiceClock() {
        return Clock.system(AppointmentSchedule.TIME_ZONE);
    }
}
