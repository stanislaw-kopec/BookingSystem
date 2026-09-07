package pl.autoserwis.vehicle.dto;

public record VehicleResponse(
    Long id,
    String make,
    String model,
    int productionYear,
    String registrationNumber,
    String vin
) {}
