package pl.autoserwis.vehicle;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class VehicleDataNormalizer {
    private static final Pattern VIN_PATTERN = Pattern.compile("^[A-HJ-NPR-Z0-9]{17}$");

    private final Clock clock;

    public VehicleDataNormalizer(Clock workshopClock) {
        this.clock = workshopClock;
    }

    public NormalizationResult normalize(String make, String model, int productionYear,
            String registrationNumber, String vin) {
        String normalizedRegistration = registrationNumber.replaceAll("\\s+", "")
            .toUpperCase(Locale.ROOT);
        String normalizedVin = optionalUppercase(vin);
        Map<String, String> errors = new LinkedHashMap<>();

        int latestAllowedYear = Year.now(clock).getValue() + 1;
        if (productionYear > latestAllowedYear) {
            errors.put("productionYear",
                "Production year cannot be later than " + latestAllowedYear + ".");
        }
        if (normalizedRegistration.length() < 2) {
            errors.put("registrationNumber",
                "Registration number must have at least 2 characters.");
        }
        if (normalizedVin != null && !VIN_PATTERN.matcher(normalizedVin).matches()) {
            errors.put("vin", "VIN must have 17 characters and cannot contain I, O or Q.");
        }

        NormalizedVehicleData data = new NormalizedVehicleData(
            make.strip(), model.strip(), productionYear, normalizedRegistration, normalizedVin);
        return new NormalizationResult(data, errors);
    }

    private String optionalUppercase(String value) {
        return value == null || value.isBlank() ? null : value.strip().toUpperCase(Locale.ROOT);
    }

    public record NormalizedVehicleData(
        String make,
        String model,
        int productionYear,
        String registrationNumber,
        String vin
    ) {}

    public record NormalizationResult(
        NormalizedVehicleData data,
        Map<String, String> fieldErrors
    ) {
        public NormalizationResult {
            fieldErrors = Map.copyOf(fieldErrors);
        }

        public boolean isValid() {
            return fieldErrors.isEmpty();
        }
    }
}
