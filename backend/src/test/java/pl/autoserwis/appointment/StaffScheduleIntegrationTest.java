package pl.autoserwis.appointment;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.PostgresTestConfiguration;
import pl.autoserwis.appointment.dto.ScheduleDayOverrideRequest;
import pl.autoserwis.appointment.dto.ScheduleSettingsRequest;
import pl.autoserwis.exception.ApiErrorCode;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static pl.autoserwis.DatabaseTestUsers.databaseUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class StaffScheduleIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AppointmentRepository appointments;
    @Autowired ScheduleDayOverrideRepository overrides;
    @Autowired WorkshopScheduleConfigService config;
    @Autowired StaffScheduleService schedule;
    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired JdbcTemplate jdbc;

    @Test
    void rejectsDefaultReductionEvenBeyondTheShortenedBookingHorizon() throws Exception {
        LocalDate date = monday().plusWeeks(8);
        add(date); add(date);
        mvc.perform(put("/api/admin/schedule/settings")
                .with(databaseUser("schedule-admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"defaultDailyCapacity":1,"bookingHorizonDays":7,
                     "workdayStart":"08:00","workdayEnd":"16:00"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SCHEDULE_CAPACITY_CONFLICT"))
            .andExpect(jsonPath("$.fieldErrors.defaultDailyCapacity").exists());
        assertThat(config.currentSettings().getDefaultDailyCapacity()).isEqualTo(4);
    }

    @Test
    void allowsLowerDefaultWhenAnOverrideStillCoversExistingBookings() {
        LocalDate date = monday();
        add(date); add(date);
        config.saveOverride(new ScheduleDayOverrideRequest(date, 2, false, "Extra capacity"));
        config.updateSettings(settings(1));
        assertThat(schedule.schedule(date, date).days().getFirst().remainingCapacity()).isZero();
        assertThat(config.currentSettings().getDefaultDailyCapacity()).isEqualTo(1);
    }

    @Test
    void cannotRemoveHigherCapacityOverrideWithTooManyBookings() {
        LocalDate date = monday();
        config.updateSettings(settings(1));
        config.saveOverride(new ScheduleDayOverrideRequest(date, 2, false, "Extra capacity"));
        add(date); add(date);
        assertThatThrownBy(() -> config.deleteOverride(date))
            .isInstanceOfSatisfying(AppointmentConflictException.class,
                error -> assertThat(error.getCode()).isEqualTo(ApiErrorCode.SCHEDULE_CAPACITY_CONFLICT));
        assertThat(overrides.findByDate(date)).isPresent();
    }

    @Test
    void cannotRemoveAnOccupiedSaturdayOpening() {
        LocalDate saturday = monday().plusDays(5);
        config.saveOverride(new ScheduleDayOverrideRequest(saturday, 2, false, "Saturday opening"));
        add(saturday);
        assertThatThrownBy(() -> config.deleteOverride(saturday)).isInstanceOf(AppointmentConflictException.class);
        assertThat(overrides.findByDate(saturday)).isPresent();
    }

    @Test
    void canRemoveSaturdayOpeningAfterCancellation() {
        LocalDate saturday = monday().plusDays(5);
        config.saveOverride(new ScheduleDayOverrideRequest(saturday, 2, false, "Saturday opening"));
        add(saturday).cancel(Instant.now());
        config.deleteOverride(saturday);
        assertThat(overrides.findByDate(saturday)).isEmpty();
        assertThat(schedule.schedule(saturday, saturday).days().getFirst().closed()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void cannotCloseOrReduceAnOccupiedDay(int capacity) {
        LocalDate date = monday();
        add(date); add(date);
        assertThatThrownBy(() -> config.saveOverride(
            new ScheduleDayOverrideRequest(date, capacity, capacity == 0, "Changed hours")))
            .isInstanceOf(AppointmentConflictException.class);
        assertThat(overrides.findByDate(date)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MECHANIC", "ADMIN"})
    void returnsTheWholeWeekWithExceptionsAndOnlyActiveAppointmentsInRange(String role) throws Exception {
        LocalDate monday = monday();
        config.saveOverride(new ScheduleDayOverrideRequest(monday.plusDays(1), 0, true, "Closed"));
        config.saveOverride(new ScheduleDayOverrideRequest(monday.plusDays(5), 2, false, "Open Saturday"));
        add(monday);
        add(monday).cancel(Instant.now());
        add(monday.plusDays(5));
        add(monday.minusDays(1));
        add(monday.plusDays(7));
        mvc.perform(get("/api/staff/appointments/schedule")
                .param("startDate", monday.toString()).param("endDate", monday.plusDays(6).toString())
                .with(databaseUser("schedule-" + role.toLowerCase()).roles(role)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timeZone").value("Europe/Warsaw"))
            .andExpect(jsonPath("$.days.length()").value(7))
            .andExpect(jsonPath("$.days[0].remainingCapacity").value(3))
            .andExpect(jsonPath("$.days[0].appointments.length()").value(1))
            .andExpect(jsonPath("$.days[0].appointments[0].problemSummary").value("Engine needs inspection"))
            .andExpect(jsonPath("$.days[0].appointments[0].contactEmail").doesNotExist())
            .andExpect(jsonPath("$.days[0].appointments[0].repairItems").doesNotExist())
            .andExpect(jsonPath("$.days[1].closed").value(true))
            .andExpect(jsonPath("$.days[1].capacity").value(0))
            .andExpect(jsonPath("$.days[5].closed").value(false))
            .andExpect(jsonPath("$.days[5].capacity").value(2))
            .andExpect(jsonPath("$.days[5].remainingCapacity").value(1))
            .andExpect(jsonPath("$.days[5].appointments.length()").value(1))
            .andExpect(jsonPath("$.days[6].closed").value(true))
            .andExpect(jsonPath("$.days[6].appointments.length()").value(0));
    }

    @Test
    void rejectsGuestsAndClients() throws Exception {
        mvc.perform(get("/api/staff/appointments/schedule")
                .param("startDate", "2030-01-01").param("endDate", "2030-01-07"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/staff/appointments/schedule")
                .param("startDate", "2030-01-01").param("endDate", "2030-01-07")
                .with(databaseUser("schedule-client").roles("CLIENT")))
            .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2029-12-31", "2030-02-01"})
    void rejectsReversedOrExcessiveRanges(String endDate) throws Exception {
        mvc.perform(get("/api/staff/appointments/schedule")
                .param("startDate", "2030-01-01").param("endDate", endDate)
                .with(databaseUser("schedule-mechanic").roles("MECHANIC")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.dateRange").exists());
    }

    @Test
    void groupsBothOccurrencesOfTheDstHourAndUsesExclusiveLocalDayBoundaries() {
        LocalDate date = LocalDate.of(2026, 10, 25);
        add(Instant.parse("2026-10-24T21:59:59Z"));
        add(Instant.parse("2026-10-24T22:00:00Z"));
        add(Instant.parse("2026-10-25T00:30:00Z"));
        add(Instant.parse("2026-10-25T01:30:00Z"));
        add(Instant.parse("2026-10-25T22:59:59Z"));
        add(Instant.parse("2026-10-25T23:00:00Z"));
        assertThat(schedule.schedule(date, date).days().getFirst().appointments()).hasSize(4);
    }

    @Test
    void calendarUsesThreeQueriesWithoutLoadingAppointmentEntitiesOrTheirRelations() {
        LocalDate date = monday();
        for (int day = 0; day < 7; day++) {
            for (int index = 0; index < 4; index++) add(date.plusDays(day));
        }
        entityManager.flush();
        entityManager.clear();
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean previouslyEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            var result = schedule.schedule(date, date.plusDays(6));
            assertThat(result.days().stream().mapToInt(day -> day.appointments().size()).sum()).isEqualTo(28);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
            assertThat(statistics.getEntityLoadCount()).isEqualTo(1); // Singleton settings only.
            assertThat(statistics.getCollectionFetchCount()).isZero();
        } finally {
            statistics.setStatisticsEnabled(previouslyEnabled);
            statistics.clear();
        }
    }

    @Test
    void rangeIndexSupportsAWeekQueryWithThousandsOfHistoricalAppointments() {
        jdbc.update("""
            insert into appointment_requests (reference, requester_type, first_name, last_name,
              phone_number, vehicle_make, vehicle_model, vehicle_production_year, vehicle_registration_number,
              requested_start_at, current_start_at, problem_description, status, created_at, updated_at)
            select gen_random_uuid(), 'GUEST', 'Test', 'Driver', '500600700', 'Toyota', 'Yaris', 2020, 'TEST',
              timestamptz '2020-01-01 08:00:00+01' + day * interval '1 day',
              timestamptz '2020-01-01 08:00:00+01' + day * interval '1 day',
              'Engine needs inspection', 'PENDING', now(), now()
            from generate_series(0, 3999) as day
            """);
        jdbc.execute("ANALYZE appointment_requests");
        String plan = String.join("\n", jdbc.queryForList("""
            explain (analyze, buffers) select id, reference, status, current_start_at,
              vehicle_make, vehicle_model, vehicle_registration_number, first_name, last_name,
              substring(problem_description, 1, 160)
            from appointment_requests
            where status in ('PENDING', 'TIME_PROPOSED', 'CONFIRMED')
              and current_start_at >= timestamptz '2026-09-14 00:00:00+02'
              and current_start_at < timestamptz '2026-09-21 00:00:00+02'
            order by current_start_at, created_at, id
            """, String.class));
        assertThat(plan).contains("ix_appointment_requests_status_start");
        System.out.println("Schedule range query plan:\n" + plan);
    }

    private AppointmentRequest add(LocalDate date) {
        return add(date.atTime(8, 0).atZone(AppointmentSchedule.TIME_ZONE).toInstant());
    }

    private AppointmentRequest add(Instant startAt) {
        return appointments.save(new AppointmentRequest(UUID.randomUUID(), AppointmentRequesterType.GUEST,
            null, null, "Anna", "Nowak", "500600700", "", "Toyota", "Yaris", 2020, "TEST", "",
            startAt, "Engine needs inspection", Instant.now()));
    }

    private LocalDate monday() {
        return LocalDate.now(AppointmentSchedule.TIME_ZONE).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private ScheduleSettingsRequest settings(int capacity) {
        return new ScheduleSettingsRequest(capacity, 30, LocalTime.of(8, 0), LocalTime.of(16, 0));
    }
}
