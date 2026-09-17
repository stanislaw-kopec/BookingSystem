package pl.autoserwis.appointment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.vehicle.Vehicle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    @Column(name = "repair_description", length = 2000)
    private String repairDescription;
    @Column(name = "total_gross_amount", precision = 10, scale = 2)
    private BigDecimal totalGrossAmount;
    @Column(name = "repair_completed_at")
    private Instant repairCompletedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repair_completed_by_id")
    private AppUser repairCompletedBy;
    @Column(name = "vehicle_picked_up_at")
    private Instant vehiclePickedUpAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_picked_up_by_id")
    private AppUser vehiclePickedUpBy;
    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemOrder ASC")
    private List<AppointmentRepairItem> repairItems = new ArrayList<>();
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
    public void cancel(Instant actionAt) {
        status = AppointmentStatus.CANCELLED;
        updatedAt = actionAt;
    }
    public void confirmGuestProposedTime(AppUser staff, Instant actionAt) {
        status = AppointmentStatus.CONFIRMED;
        recordStaffAction(staff, actionAt, staffMessage);
    }
    public void completeRepair(AppUser staff, String repairDescription,
            List<RepairItemDraft> items, Instant actionAt) {
        BigDecimal validatedTotal = RepairAmounts.validatedTotal(items);
        status = AppointmentStatus.READY_FOR_PICKUP;
        this.repairDescription = repairDescription;
        repairItems.clear();
        for (int index = 0; index < items.size(); index++) {
            AppointmentRepairItem item = new AppointmentRepairItem(this, index + 1, items.get(index));
            repairItems.add(item);
        }
        totalGrossAmount = validatedTotal;
        repairCompletedAt = actionAt;
        repairCompletedBy = staff;
        updatedAt = actionAt;
    }
    public void markPickedUp(AppUser staff, Instant actionAt) {
        status = AppointmentStatus.COMPLETED;
        vehiclePickedUpAt = actionAt;
        vehiclePickedUpBy = staff;
        updatedAt = actionAt;
    }
    private void recordStaffAction(AppUser staff, Instant actionAt, String message) {
        staffActionBy = staff;
        staffActionAt = actionAt;
        staffMessage = message;
        updatedAt = actionAt;
    }
}
