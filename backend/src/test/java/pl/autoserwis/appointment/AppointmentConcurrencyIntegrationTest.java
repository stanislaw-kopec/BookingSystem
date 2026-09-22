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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
class AppointmentConcurrencyIntegrationTest {
    @Autowired AppointmentBookingService appointmentService;
    @Autowired AppointmentRepository appointments;

    @BeforeEach
    @AfterEach
    void clearAppointments() {
        appointments.deleteAll();
    }

    @Test
    void allowsOnlyDailyCapacityForConcurrentRequestsOnTheSameDay() throws Exception {
        LocalDate visitDate = workingDate();
        CountDownLatch ready = new CountDownLatch(AppointmentSchedule.DAILY_CAPACITY + 1);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(AppointmentSchedule.DAILY_CAPACITY + 1);

        try {
            List<Future<AttemptResult>> attempts = IntStream.rangeClosed(1, AppointmentSchedule.DAILY_CAPACITY + 1)
                .mapToObj(index -> executor.submit(attempt(visitDate, index, ready, start)))
                .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<AttemptResult> results = attempts.stream()
                .map(future -> result(future))
                .toList();

            assertThat(results).filteredOn(AttemptResult::succeeded)
                .hasSize(AppointmentSchedule.DAILY_CAPACITY);
            assertThat(results).filteredOn(result -> !result.succeeded())
                .singleElement()
                .extracting(AttemptResult::failure)
                .isInstanceOf(AppointmentConflictException.class);
            assertThat(appointments.count()).isEqualTo(AppointmentSchedule.DAILY_CAPACITY);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<AttemptResult> attempt(LocalDate visitDate, int index,
            CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                return AttemptResult.failure(new IllegalStateException("Nie uruchomiono próby na czas."));
            }
            try {
                appointmentService.createForGuest(request(visitDate, index));
                return AttemptResult.success();
            } catch (RuntimeException exception) {
                return AttemptResult.failure(exception);
            }
        };
    }

    private AttemptResult result(Future<AttemptResult> future) {
        try {
            return future.get(15, TimeUnit.SECONDS);
        } catch (Exception exception) {
            return AttemptResult.failure(exception);
        }
    }

    private GuestAppointmentRequest request(LocalDate visitDate, int index) {
        return new GuestAppointmentRequest(
            "Anna", "Nowak", "+48 500 600 700", "",
            "Toyota", "Yaris", 2020, "KR" + (100 + index), "",
            visitDate, "Silnik nierówno pracuje po uruchomieniu.");
    }

    private LocalDate workingDate() {
        LocalDate date = LocalDate.now(AppointmentSchedule.TIME_ZONE).plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
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
