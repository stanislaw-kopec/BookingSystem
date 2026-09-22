package pl.autoserwis.appointment;

import org.junit.jupiter.api.Test;
import pl.autoserwis.appointment.domain.AppointmentConflictException;
import pl.autoserwis.appointment.domain.AppointmentRequest;
import pl.autoserwis.appointment.domain.AppointmentRequesterType;
import pl.autoserwis.appointment.domain.AppointmentStatus;
import pl.autoserwis.appointment.domain.RepairItemDraft;
import pl.autoserwis.appointment.domain.RepairItemType;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentRequestTest {
    private static final Instant CREATED_AT = Instant.parse("2026-09-20T08:00:00Z");
    private static final Instant VISIT_AT = Instant.parse("2026-09-22T06:00:00Z");

    @Test
    void followsTheRepairLifecycleFromPendingToCompleted() {
        AppointmentRequest appointment = guestAppointment();
        AppUser staff = staff();
        Instant acceptedAt = CREATED_AT.plusSeconds(60);
        Instant repairedAt = CREATED_AT.plusSeconds(120);
        Instant pickedUpAt = CREATED_AT.plusSeconds(180);

        appointment.accept(staff, acceptedAt);
        appointment.completeRepair(staff, "Replaced the damaged ignition coil.", List.of(
            new RepairItemDraft(RepairItemType.LABOR, "Diagnosis", BigDecimal.ONE,
                new BigDecimal("100.00")),
            new RepairItemDraft(RepairItemType.PART, "Ignition coil", BigDecimal.ONE,
                new BigDecimal("150.00"))), repairedAt);
        appointment.markPickedUp(staff, pickedUpAt);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.getRepairCompletedAt()).isEqualTo(repairedAt);
        assertThat(appointment.getVehiclePickedUpAt()).isEqualTo(pickedUpAt);
        assertThat(appointment.getTotalGrossAmount()).isEqualByComparingTo("250.00");
        assertThat(appointment.getRepairItems()).hasSize(2);
    }

    @Test
    void rejectsRepairAndPickupOutsideTheirRequiredStatuses() {
        AppointmentRequest appointment = guestAppointment();

        assertThatThrownBy(() -> appointment.completeRepair(staff(), "Completed repair work.",
            List.of(new RepairItemDraft(RepairItemType.LABOR, "Diagnosis", BigDecimal.ONE,
                BigDecimal.TEN)), CREATED_AT.plusSeconds(60)))
            .isInstanceOf(AppointmentConflictException.class);
        assertThatThrownBy(() -> appointment.markPickedUp(staff(), CREATED_AT.plusSeconds(120)))
            .isInstanceOf(AppointmentConflictException.class);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(appointment.getRepairItems()).isEmpty();
        assertThat(appointment.getVehiclePickedUpAt()).isNull();
    }

    @Test
    void confirmsOnlyAnExistingDayProposal() {
        AppointmentRequest appointment = guestAppointment();
        Instant proposedAt = VISIT_AT.plusSeconds(86_400);

        assertThatThrownBy(() -> appointment.confirmProposedTime(CREATED_AT.plusSeconds(60)))
            .isInstanceOf(AppointmentConflictException.class);

        appointment.proposeTime(staff(), proposedAt, "Alternative day", CREATED_AT.plusSeconds(120));
        appointment.confirmProposedTime(CREATED_AT.plusSeconds(180));

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(appointment.getCurrentStartAt()).isEqualTo(proposedAt);
        assertThat(appointment.getClientConfirmedAt()).isEqualTo(CREATED_AT.plusSeconds(180));
        assertThatThrownBy(() -> appointment.confirmProposedTime(CREATED_AT.plusSeconds(240)))
            .isInstanceOf(AppointmentConflictException.class);
    }

    @Test
    void staffCannotConfirmAProposalForARegisteredClient() {
        AppUser client = new AppUser("client", "client@example.test", "unused", UserRole.CLIENT);
        AppointmentRequest appointment = appointment(AppointmentRequesterType.CLIENT, client);
        appointment.proposeTime(staff(), VISIT_AT.plusSeconds(86_400), null,
            CREATED_AT.plusSeconds(60));

        assertThatThrownBy(() -> appointment.confirmGuestProposedTime(
            staff(), CREATED_AT.plusSeconds(120)))
            .isInstanceOf(AppointmentConflictException.class);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.TIME_PROPOSED);
    }

    @Test
    void cancellationWorksOnlyForActiveAppointments() {
        AppointmentRequest appointment = guestAppointment();

        appointment.cancel(CREATED_AT.plusSeconds(60));

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThatThrownBy(() -> appointment.cancel(CREATED_AT.plusSeconds(120)))
            .isInstanceOf(AppointmentConflictException.class);
    }

    private AppointmentRequest guestAppointment() {
        return appointment(AppointmentRequesterType.GUEST, null);
    }

    private AppointmentRequest appointment(AppointmentRequesterType requesterType, AppUser client) {
        return new AppointmentRequest(UUID.randomUUID(), requesterType, client, null,
            "Anna", "Nowak", "500600700", "anna@example.test",
            "Toyota", "Yaris", 2020, "KR12345", null,
            VISIT_AT, "Engine needs inspection.", CREATED_AT);
    }

    private AppUser staff() {
        return new AppUser("mechanic", "mechanic@example.test", "unused", UserRole.MECHANIC);
    }
}
