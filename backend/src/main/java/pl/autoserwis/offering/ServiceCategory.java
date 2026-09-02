package pl.autoserwis.offering;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "service_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    public ServiceCategory(String name, String description) {
        update(name, description);
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
