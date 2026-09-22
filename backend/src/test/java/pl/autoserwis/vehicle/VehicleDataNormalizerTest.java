package pl.autoserwis.vehicle;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleDataNormalizerTest {
    private final VehicleDataNormalizer normalizer = new VehicleDataNormalizer(
        Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneId.of("Europe/Warsaw")));

    @Test
    void normalizesSharedVehicleFields() {
        VehicleDataNormalizer.NormalizationResult result = normalizer.normalize(
            "  Honda  ", "  Civic  ", 2020, " kr 12 34 ", " wvwzzz1jzxw000001 ");

        assertThat(result.isValid()).isTrue();
        assertThat(result.data()).isEqualTo(new VehicleDataNormalizer.NormalizedVehicleData(
            "Honda", "Civic", 2020, "KR1234", "WVWZZZ1JZXW000001"));
    }

    @Test
    void reportsDynamicYearRegistrationAndVinErrors() {
        VehicleDataNormalizer.NormalizationResult result = normalizer.normalize(
            "Honda", "Civic", 2028, " ", "INVALID");

        assertThat(result.fieldErrors())
            .containsEntry("productionYear", "Production year cannot be later than 2027.")
            .containsEntry("registrationNumber", "Registration number must have at least 2 characters.")
            .containsEntry("vin", "VIN must have 17 characters and cannot contain I, O or Q.");
    }
}
