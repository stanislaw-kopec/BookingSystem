package pl.autoserwis.offering;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "workshop_services")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkshopService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    public WorkshopService(ServiceCategory category, String name, String description) {
        update(category, name, description);
    }

    public void update(ServiceCategory category, String name, String description) {
        this.category = category;
        this.name = name;
        this.description = description;
    }
}
