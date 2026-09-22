package pl.autoserwis.appointment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pl.autoserwis.PostgresTestConfiguration;
import pl.autoserwis.appointment.api.dto.GuestAppointmentRequest;
import pl.autoserwis.appointment.api.dto.ProposeAppointmentTimeRequest;
import pl.autoserwis.appointment.application.AppointmentBookingService;
import pl.autoserwis.appointment.application.StaffAppointmentService;
import pl.autoserwis.appointment.domain.AppointmentConflictException;
import pl.autoserwis.appointment.domain.AppointmentRequest;
import pl.autoserwis.appointment.domain.AppointmentStatus;
import pl.autoserwis.appointment.domain.AppointmentValidationException;
import pl.autoserwis.appointment.persistence.AppointmentRepository;
import pl.autoserwis.appointment.schedule.AppointmentSchedule;
import pl.autoserwis.appointment.schedule.WorkshopScheduleConfigService;
import pl.autoserwis.appointment.schedule.domain.WorkshopScheduleSettings;
import pl.autoserwis.appointment.schedule.dto.ScheduleDayOverrideRequest;
import pl.autoserwis.appointment.schedule.dto.ScheduleSettingsRequest;
import pl.autoserwis.appointment.schedule.persistence.ScheduleDayOverrideRepository;
import pl.autoserwis.appointment.schedule.persistence.WorkshopScheduleSettingsRepository;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;
import pl.autoserwis.user.UserRole;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
class ScheduleConcurrencyIntegrationTest {
    @Autowired AppointmentBookingService booking;
    @Autowired StaffAppointmentService staffAppointments;
    @Autowired AppointmentRepository appointments;
    @Autowired WorkshopScheduleConfigService config;
    @Autowired WorkshopScheduleSettingsRepository settings;
    @Autowired ScheduleDayOverrideRepository overrides;
    @Autowired UserRepository users;
    @Autowired PlatformTransactionManager transactionManager;
    private Long staffId;

    @BeforeEach
    @AfterEach
    void resetFixtures() {
        appointments.deleteAll();
        overrides.deleteAll();
        settings.save(new WorkshopScheduleSettings(4, 30, LocalTime.of(8, 0), LocalTime.of(16, 0)));
        if (staffId != null) {
            users.deleteById(staffId);
            staffId = null;
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void defaultCapacityChangeAndBookingRespectCommitOrder(boolean bookingFirst) throws Exception {
        LocalDate date = monday();
        booking.createForGuest(request(date));
        Runnable reserve = () -> booking.createForGuest(request(date));
        Runnable reduce = () -> config.updateSettings(settingsRequest(1, 8));
        RuntimeException failure = bookingFirst ? runOverlapping(reserve, reduce) : runOverlapping(reduce, reserve);
        assertThat(failure).isInstanceOf(AppointmentConflictException.class);
        assertThat(appointments.count()).isEqualTo(bookingFirst ? 2 : 1);
        assertThat(config.currentSettings().getDefaultDailyCapacity()).isEqualTo(bookingFirst ? 4 : 1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void overrideRemovalAndBookingRespectCommitOrder(boolean bookingFirst) throws Exception {
        LocalDate date = monday();
        config.updateSettings(settingsRequest(1, 8));
        config.saveOverride(new ScheduleDayOverrideRequest(date, 2, false, "Extra capacity"));
        booking.createForGuest(request(date));
        Runnable reserve = () -> booking.createForGuest(request(date));
        Runnable remove = () -> config.deleteOverride(date);
        RuntimeException failure = bookingFirst ? runOverlapping(reserve, remove) : runOverlapping(remove, reserve);
        assertThat(failure).isInstanceOf(AppointmentConflictException.class);
        assertThat(appointments.count()).isEqualTo(bookingFirst ? 2 : 1);
        assertThat(overrides.findByDate(date).isPresent()).isEqualTo(bookingFirst);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void closingADayAndBookingRespectCommitOrder(boolean bookingFirst) throws Exception {
        LocalDate date = monday();
        Runnable reserve = () -> booking.createForGuest(request(date));
        Runnable close = () -> config.saveOverride(new ScheduleDayOverrideRequest(date, 0, true, "Closed"));
        RuntimeException failure = bookingFirst ? runOverlapping(reserve, close) : runOverlapping(close, reserve);
        assertThat(failure).isInstanceOf(bookingFirst ? AppointmentConflictException.class : AppointmentValidationException.class);
        assertThat(appointments.count()).isEqualTo(bookingFirst ? 1 : 0);
        assertThat(overrides.findByDate(date).isPresent()).isEqualTo(!bookingFirst);
    }

    @Test
    void bookingReadsNewWorkingHoursOnlyAfterConfigurationCommits() throws Exception {
        LocalDate date = monday();
        assertThat(runOverlapping(() -> config.updateSettings(settingsRequest(4, 9)),
            () -> booking.createForGuest(request(date)))).isNull();
        assertThat(appointments.findAll()).singleElement().satisfies(appointment ->
            assertThat(appointment.getCurrentStartAt()).isEqualTo(
                date.atTime(9, 0).atZone(AppointmentSchedule.TIME_ZONE).toInstant()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void proposingAnotherDayAndClosingItRespectCommitOrder(boolean proposalFirst) throws Exception {
        LocalDate original = monday();
        LocalDate target = original.plusDays(1);
        staffId = users.save(new AppUser("schedule-race-staff", "schedule-race@example.test", "unused", UserRole.MECHANIC)).getId();
        Long appointmentId = booking.createForGuest(request(original)).id();
        Runnable propose = () -> staffAppointments.proposeTime(staffId, appointmentId,
            new ProposeAppointmentTimeRequest(target, "New day"));
        Runnable close = () -> config.saveOverride(new ScheduleDayOverrideRequest(target, 0, true, "Closed"));
        RuntimeException failure = proposalFirst ? runOverlapping(propose, close) : runOverlapping(close, propose);
        assertThat(failure).isInstanceOf(proposalFirst ? AppointmentConflictException.class : AppointmentValidationException.class);
        AppointmentRequest saved = appointments.findById(appointmentId).orElseThrow();
        assertThat(saved.getCurrentStartAt().atZone(AppointmentSchedule.TIME_ZONE).toLocalDate())
            .isEqualTo(proposalFirst ? target : original);
        assertThat(saved.getStatus()).isEqualTo(proposalFirst ? AppointmentStatus.TIME_PROPOSED : AppointmentStatus.PENDING);
    }

    // Hold the first transaction open after its write. The second operation must wait
    // for commit and then validate against committed state, rather than a stale snapshot.
    private RuntimeException runOverlapping(Runnable first, Runnable second) throws Exception {
        CountDownLatch firstWritten = new CountDownLatch(1);
        CountDownLatch commitFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            try {
                Future<?> firstResult = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(transaction -> {
                        first.run();
                        firstWritten.countDown();
                        await(commitFirst);
                    }));
                assertThat(firstWritten.await(10, TimeUnit.SECONDS)).isTrue();
                Future<RuntimeException> secondResult = executor.submit(() -> {
                    secondStarted.countDown();
                    try { second.run(); return null; }
                    catch (RuntimeException failure) { return failure; }
                });
                assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> secondResult.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                commitFirst.countDown();
                firstResult.get(10, TimeUnit.SECONDS);
                return secondResult.get(10, TimeUnit.SECONDS);
            } finally {
                commitFirst.countDown();
                executor.shutdownNow();
            }
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for commit");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted before commit", exception);
        }
    }

    private LocalDate monday() {
        return LocalDate.now(AppointmentSchedule.TIME_ZONE).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private ScheduleSettingsRequest settingsRequest(int capacity, int startHour) {
        return new ScheduleSettingsRequest(capacity, 30, LocalTime.of(startHour, 0), LocalTime.of(16, 0));
    }

    private GuestAppointmentRequest request(LocalDate date) {
        return new GuestAppointmentRequest("Anna", "Nowak", "500600700", "", "Toyota", "Yaris",
            2020, "KR12345", "", date, "Engine needs inspection");
    }
}
