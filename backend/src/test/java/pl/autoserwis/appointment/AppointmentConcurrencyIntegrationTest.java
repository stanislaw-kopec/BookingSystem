package pl.autoserwis.appointment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import pl.autoserwis.PostgresTestConfiguration;
import pl.autoserwis.appointment.dto.GuestAppointmentRequest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
class AppointmentConcurrencyIntegrationTest {
    @Autowired AppointmentService appointmentService;
    @Autowired AppointmentRepository appointments;

    @BeforeEach
    @AfterEach
    void clearAppointments() {
        appointments.deleteAll();
    }

    @Test
    void allowsOnlyOneOfTwoConcurrentRequestsForTheSameSlot() throws Exception {
        GuestAppointmentRequest request = request(workingSlot());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<AttemptResult> attempt = () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    return AttemptResult.failure(new IllegalStateException("Nie uruchomiono próby na czas."));
                }
                try {
                    appointmentService.createForGuest(request);
                    return AttemptResult.success();
                } catch (RuntimeException exception) {
                    return AttemptResult.failure(exception);
                }
            };

            Future<AttemptResult> first = executor.submit(attempt);
            Future<AttemptResult> second = executor.submit(attempt);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<AttemptResult> results = List.of(
                first.get(15, TimeUnit.SECONDS),
                second.get(15, TimeUnit.SECONDS));

            assertThat(results).filteredOn(AttemptResult::succeeded).hasSize(1);
            assertThat(results).filteredOn(result -> !result.succeeded())
                .singleElement()
                .extracting(AttemptResult::failure)
                .isInstanceOf(AppointmentConflictException.class);
            assertThat(appointments.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private GuestAppointmentRequest request(OffsetDateTime startAt) {
        return new GuestAppointmentRequest(
            "Anna", "Nowak", "+48 500 600 700", "",
            "Toyota", "Yaris", 2020, "KR123", "",
            startAt, "Silnik nierówno pracuje po uruchomieniu.");
    }

    private OffsetDateTime workingSlot() {
        LocalDate date = LocalDate.now(AppointmentSchedule.TIME_ZONE).plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date.atTime(8, 0).atZone(AppointmentSchedule.TIME_ZONE).toOffsetDateTime();
    }

    private record AttemptResult(boolean succeeded, Throwable failure) {
        static AttemptResult success() {
            return new AttemptResult(true, null);
        }

        static AttemptResult failure(Throwable failure) {
            return new AttemptResult(false, failure);
        }
    }
}
