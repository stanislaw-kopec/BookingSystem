package pl.autoserwis.vehicle;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.autoserwis.user.AppUser;

@Entity
@Table(name = "vehicles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Column(nullable = false, length = 80)
    private String make;

    @Column(nullable = false, length = 80)
    private String model;

    @Column(name = "production_year", nullable = false)
    private int productionYear;

    @Column(name = "registration_number", nullable = false, length = 20)
    private String registrationNumber;

    @Column(length = 17)
    private String vin;

    public Vehicle(AppUser owner, String make, String model, int productionYear,
            String registrationNumber, String vin) {
        this.owner = owner;
        this.make = make;
        this.model = model;
        this.productionYear = productionYear;
        this.registrationNumber = registrationNumber;
        this.vin = vin;
    }
}
