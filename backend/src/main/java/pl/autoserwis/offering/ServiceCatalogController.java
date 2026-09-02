package pl.autoserwis.offering;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.autoserwis.offering.dto.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ServiceCatalogController {
    private final ServiceCatalogService catalog;

    public ServiceCatalogController(ServiceCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/services")
    public List<ServiceCategoryResponse> getCatalog() {
        return catalog.getCatalog();
    }

    @GetMapping("/services/{id}")
    public WorkshopServiceResponse getService(@PathVariable Long id) {
        return catalog.getService(id);
    }

    @GetMapping("/service-categories/{id}")
    public ServiceCategoryResponse getCategory(@PathVariable Long id) {
        return catalog.getCategory(id);
    }

    @PostMapping("/service-categories")
    public ResponseEntity<ServiceCategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        ServiceCategoryResponse result = catalog.createCategory(request);
        return ResponseEntity.created(URI.create("/api/service-categories/" + result.id())).body(result);
    }

    @PutMapping("/service-categories/{id}")
    public ServiceCategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return catalog.updateCategory(id, request);
    }

    @DeleteMapping("/service-categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        catalog.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/services")
    public ResponseEntity<WorkshopServiceResponse> createService(@Valid @RequestBody WorkshopServiceRequest request) {
        WorkshopServiceResponse result = catalog.createService(request);
        return ResponseEntity.created(URI.create("/api/services/" + result.id())).body(result);
    }

    @PutMapping("/services/{id}")
    public WorkshopServiceResponse updateService(@PathVariable Long id, @Valid @RequestBody WorkshopServiceRequest request) {
        return catalog.updateService(id, request);
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        catalog.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}
