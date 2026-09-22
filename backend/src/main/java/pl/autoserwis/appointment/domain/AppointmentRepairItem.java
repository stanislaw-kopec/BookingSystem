package pl.autoserwis.appointment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "appointment_repair_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppointmentRepairItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_request_id", nullable = false)
    private AppointmentRequest appointment;

    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RepairItemType type;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_gross_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitGrossAmount;

    @Column(name = "total_gross_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalGrossAmount;

    public AppointmentRepairItem(AppointmentRequest appointment, int itemOrder, RepairItemDraft draft) {
        this.appointment = appointment;
        this.itemOrder = itemOrder;
        this.type = draft.type();
        this.name = draft.name();
        this.quantity = draft.quantity();
        this.unitGrossAmount = draft.unitGrossAmount();
        this.totalGrossAmount = RepairAmounts.lineGross(draft.quantity(), draft.unitGrossAmount());
    }
}
