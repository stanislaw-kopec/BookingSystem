package pl.autoserwis.offering;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.exception.ResourceConflictException;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.offering.dto.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ServiceCatalogService {
    private final ServiceCategoryRepository categories;
    private final WorkshopServiceRepository services;

    public ServiceCatalogService(ServiceCategoryRepository categories, WorkshopServiceRepository services) {
        this.categories = categories;
        this.services = services;
    }

    public List<ServiceCategoryResponse> getCatalog() {
        // Two queries fetch the whole catalogue without one query per category.
        Map<Long, List<WorkshopServiceResponse>> grouped = services.findAllByOrderByNameAsc().stream()
            .collect(Collectors.groupingBy(service -> service.getCategory().getId(),
                Collectors.mapping(this::toResponse, Collectors.toList())));
        return categories.findAllByOrderByNameAsc().stream()
            .map(category -> new ServiceCategoryResponse(category.getId(), category.getName(),
                category.getDescription(), grouped.getOrDefault(category.getId(), List.of())))
            .toList();
    }

    public ServiceCategoryResponse getCategory(Long id) {
        ServiceCategory category = category(id);
        return new ServiceCategoryResponse(id, category.getName(), category.getDescription(),
            services.findByCategory_IdOrderByNameAsc(id).stream().map(this::toResponse).toList());
    }

    public WorkshopServiceResponse getService(Long id) {
        return toResponse(service(id));
    }

    @Transactional
    public ServiceCategoryResponse createCategory(CategoryRequest request) {
        String name = request.name().strip();
        if (categories.existsByNameIgnoreCase(name)) {
            throw new ResourceConflictException("A category with this name already exists.");
        }
        ServiceCategory category = categories.save(new ServiceCategory(name, description(request.description())));
        return new ServiceCategoryResponse(category.getId(), category.getName(), category.getDescription(), List.of());
    }

    @Transactional
    public ServiceCategoryResponse updateCategory(Long id, CategoryRequest request) {
        ServiceCategory category = category(id);
        String name = request.name().strip();
        if (categories.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResourceConflictException("A category with this name already exists.");
        }
        category.update(name, description(request.description()));
        return getCategory(id);
    }

    @Transactional
    public void deleteCategory(Long id) {
        ServiceCategory category = category(id);
        if (services.existsByCategory_Id(id)) {
            throw new ResourceConflictException("Move or delete services from this category first.");
        }
        categories.delete(category);
    }

    @Transactional
    public WorkshopServiceResponse createService(WorkshopServiceRequest request) {
        ServiceCategory category = category(request.categoryId());
        String name = request.name().strip();
        if (services.existsByCategory_IdAndNameIgnoreCase(category.getId(), name)) {
            throw new ResourceConflictException("A service with this name already exists in the selected category.");
        }
        return toResponse(services.save(new WorkshopService(category, name, description(request.description()))));
    }

    @Transactional
    public WorkshopServiceResponse updateService(Long id, WorkshopServiceRequest request) {
        WorkshopService service = service(id);
        ServiceCategory category = category(request.categoryId());
        String name = request.name().strip();
        if (services.existsByCategory_IdAndNameIgnoreCaseAndIdNot(category.getId(), name, id)) {
            throw new ResourceConflictException("A service with this name already exists in the selected category.");
        }
        service.update(category, name, description(request.description()));
        return toResponse(service);
    }

    @Transactional
    public void deleteService(Long id) {
        services.delete(service(id));
    }

    private ServiceCategory category(Long id) {
        return categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found."));
    }

    private WorkshopService service(Long id) {
        return services.findById(id).orElseThrow(() -> new ResourceNotFoundException("Service not found."));
    }

    private WorkshopServiceResponse toResponse(WorkshopService service) {
        return new WorkshopServiceResponse(service.getId(), service.getCategory().getId(),
            service.getName(), service.getDescription());
    }

    private String description(String value) {
        return value == null ? "" : value.strip();
    }
}
