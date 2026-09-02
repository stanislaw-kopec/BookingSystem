package pl.autoserwis.offering;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkshopServiceRepository extends JpaRepository<WorkshopService, Long> {
    List<WorkshopService> findAllByOrderByNameAsc();
    List<WorkshopService> findByCategory_IdOrderByNameAsc(Long categoryId);
    boolean existsByCategory_Id(Long categoryId);
    boolean existsByCategory_IdAndNameIgnoreCase(Long categoryId, String name);
    boolean existsByCategory_IdAndNameIgnoreCaseAndIdNot(Long categoryId, String name, Long id);
}
