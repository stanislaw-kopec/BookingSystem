package pl.autoserwis.offering.dto;

import java.util.List;

public record ServiceCategoryResponse(
    Long id, String name, String description, List<WorkshopServiceResponse> services
) {}
