package pl.autoserwis.appointment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.vehicle.Vehicle;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appointment_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppointmentRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "requester_type", nullable = false, length = 20)
    private AppointmentRequesterType requesterType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private AppUser client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "contact_email", length = 254)
    private String contactEmail;

    @Column(name = "vehicle_make", nullable = false, length = 80)
    private String vehicleMake;

    @Column(name = "vehicle_model", nullable = false, length = 80)
    private String vehicleModel;

    @Column(name = "vehicle_production_year", nullable = false)
    private int vehicleProductionYear;

    @Column(name = "vehicle_registration_number", nullable = false, length = 20)
    private String vehicleRegistrationNumber;

    @Column(name = "vehicle_vin", length = 17)
    private String vehicleVin;

    @Column(name = "requested_start_at", nullable = false)
    private Instant requestedStartAt;

    @Column(name = "current_start_at", nullable = false)
    private Instant currentStartAt;

    @Column(name = "problem_description", nullable = false, length = 2000)
    private String problemDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AppointmentStatus status;

    @Column(name = "staff_message", length = 500)
    private String staffMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "staff_action_at")
    private Instant staffActionAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_action_by_id")
    private AppUser staffActionBy;

    @Column(name = "client_confirmed_at")
    private Instant clientConfirmedAt;

    public AppointmentRequest(UUID reference, AppointmentRequesterType requesterType,
            AppUser client, Vehicle vehicle, String firstName, String lastName,
            String phoneNumber, String contactEmail, String vehicleMake, String vehicleModel,
            int vehicleProductionYear, String vehicleRegistrationNumber, String vehicleVin,
            Instant requestedStartAt, String problemDescription, Instant createdAt) {
        this.reference = reference;
        this.requesterType = requesterType;
        this.client = client;
        this.vehicle = vehicle;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.contactEmail = contactEmail;
        this.vehicleMake = vehicleMake;
        this.vehicleModel = vehicleModel;
        this.vehicleProductionYear = vehicleProductionYear;
        this.vehicleRegistrationNumber = vehicleRegistrationNumber;
        this.vehicleVin = vehicleVin;
        this.requestedStartAt = requestedStartAt;
        this.currentStartAt = requestedStartAt;
        this.problemDescription = problemDescription;
        this.status = AppointmentStatus.PENDING;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void accept(AppUser staff, Instant actionAt) {
        status = AppointmentStatus.CONFIRMED;
        recordStaffAction(staff, actionAt, null);
    }

    public void reject(AppUser staff, String message, Instant actionAt) {
        status = AppointmentStatus.REJECTED;
        recordStaffAction(staff, actionAt, message);
    }

    public void proposeTime(AppUser staff, Instant proposedStartAt, String message, Instant actionAt) {
        currentStartAt = proposedStartAt;
        status = AppointmentStatus.TIME_PROPOSED;
        clientConfirmedAt = null;
        recordStaffAction(staff, actionAt, message);
    }

    public void confirmProposedTime(Instant actionAt) {
        status = AppointmentStatus.CONFIRMED;
        clientConfirmedAt = actionAt;
        updatedAt = actionAt;
    }

    public void confirmGuestProposedTime(AppUser staff, Instant actionAt) {
        status = AppointmentStatus.CONFIRMED;
        recordStaffAction(staff, actionAt, staffMessage);
    }

    private void recordStaffAction(AppUser staff, Instant actionAt, String message) {
        staffActionBy = staff;
        staffActionAt = actionAt;
        staffMessage = message;
        updatedAt = actionAt;
    }
}
