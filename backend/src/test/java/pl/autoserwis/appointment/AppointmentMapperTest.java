package pl.autoserwis.appointment;

import org.junit.jupiter.api.Test;
import pl.autoserwis.appointment.dto.AppointmentResponse;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRole;
import pl.autoserwis.vehicle.dto.RepairHistoryEntryResponse;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentMapperTest {
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-12T10:00:00Z"), WARSAW);

    private final RepairItemResponseMapper repairItems = new RepairItemResponseMapper();
    private final AppointmentResponseMapper appointments = new AppointmentResponseMapper(CLOCK, repairItems);
    private final RepairHistoryMapper repairHistory = new RepairHistoryMapper(CLOCK, repairItems);

    @Test
    void mapsOptionalTextAndTimestampsUsingWorkshopTimeZone() {
        AppointmentRequest appointment = pendingAppointment();

        AppointmentResponse result = appointments.toResponse(appointment);

        assertThat(result.status()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(result.vehicleId()).isNull();
        assertThat(result.phoneNumber()).isEmpty();
        assertThat(result.contactEmail()).isEmpty();
        assertThat(result.staffMessage()).isEmpty();
        assertThat(result.requestedStartAt().toString()).isEqualTo("2026-01-12T08:00+01:00");
        assertThat(result.createdAt().toString()).isEqualTo("2026-01-10T10:00+01:00");
    }

    @Test
    void mapsCompletedRepairHistoryAndItemsInOnePlace() {
        AppointmentRequest appointment = pendingAppointment();
        AppUser mechanic = new AppUser("mechanic", "mechanic@example.com", "hash", UserRole.MECHANIC);
        appointment.accept(mechanic, Instant.parse("2026-01-10T10:30:00Z"));
        appointment.completeRepair(mechanic, "Wymieniono zużyty element i sprawdzono działanie.", List.of(
            new RepairItemDraft(RepairItemType.PART, "Element", new BigDecimal("2.00"),
                new BigDecimal("75.00"))), Instant.parse("2026-01-12T13:00:00Z"));
        appointment.markPickedUp(mechanic, Instant.parse("2026-01-12T15:00:00Z"));

        RepairHistoryEntryResponse result = repairHistory.toResponse(appointment);

        assertThat(result.totalGrossAmount()).isEqualByComparingTo("150.00");
        assertThat(result.repairCompletedBy()).isEqualTo("mechanic");
        assertThat(result.vehiclePickedUpBy()).isEqualTo("mechanic");
        assertThat(result.repairItems()).singleElement().satisfies(item -> {
            assertThat(item.type()).isEqualTo(RepairItemType.PART);
            assertThat(item.name()).isEqualTo("Element");
            assertThat(item.totalGrossAmount()).isEqualByComparingTo("150.00");
        });
    }

    private AppointmentRequest pendingAppointment() {
        return new AppointmentRequest(
            UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
            AppointmentRequesterType.GUEST,
            null,
            null,
            "Jan",
            "Kowalski",
            null,
            null,
            "Honda",
            "Civic",
            2018,
            "DW12345",
            null,
            Instant.parse("2026-01-12T07:00:00Z"),
            "Silnik pracuje nierówno po uruchomieniu.",
            Instant.parse("2026-01-10T09:00:00Z"));
    }
}
