package pl.bartoszsredzinski.air_quality_map.source;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.bartoszsredzinski.air_quality_map.measurement.Parameter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StationReadingTest{

    private static final ParameterReading PM10 = new ParameterReading(Parameter.PM10, 12.5, "µg/m³", Instant.parse("2026-11-18T19:00:00Z"));
    private static final ParameterReading NO2 = new ParameterReading(Parameter.NO2, 2.4, "µg/m³", Instant.parse("2026-11-18T19:00:00Z"));

    @Test
    void keepsGivenValues(){
        StationReading station = new StationReading("11", "Czerniawa", 50.912475, 15.31219, List.of(PM10, NO2));

        assertThat(station.externalId()).isEqualTo("11");
        assertThat(station.name()).isEqualTo("Czerniawa");
        assertThat(station.lat()).isEqualTo(50.912475);
        assertThat(station.lon()).isEqualTo(15.31219);
        assertThat(station.readings()).containsExactly(PM10, NO2);
    }

    // Kontrakt AirQualitySource.fetch(): stacja znana, ale bez pomiarów (błąd pobierania) nadal jest poprawnym odczytem.
    @Test
    void acceptsStationWithoutReadings(){
        StationReading station = new StationReading("11", "Czerniawa", 50.9, 15.3, List.of());

        assertThat(station.readings()).isEmpty();
    }

    // Zakres współrzędnych (np. granice Polski) sprawdza filtr w warstwie ingest, nie model.
    @Test
    void leavesCoordinateRangeChecksToTheIngestFilter(){
        StationReading station = new StationReading("11", "Poza zakresem", 100.0, 200.0, List.of());

        assertThat(station.lat()).isEqualTo(100.0);
        assertThat(station.lon()).isEqualTo(200.0);
    }

    @Test
    void copiesReadingsDefensively(){
        List<ParameterReading> source = new ArrayList<>(List.of(PM10));

        StationReading station = new StationReading("11", "Czerniawa", 50.9, 15.3, source);
        source.add(NO2);
        source.remove(PM10);

        assertThat(station.readings()).containsExactly(PM10);
    }

    @Test
    void exposesUnmodifiableReadings(){
        StationReading station = new StationReading("11", "Czerniawa", 50.9, 15.3, new ArrayList<>(List.of(PM10)));

        assertThatThrownBy(() -> station.readings().add(NO2)).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void rejectsBlankExternalId(String externalId){
        assertThatIllegalArgumentException().isThrownBy(() -> new StationReading(externalId, "Czerniawa", 50.9, 15.3, List.of()))
                .withMessage("externalId must not be blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void rejectsBlankName(String name){
        assertThatIllegalArgumentException().isThrownBy(() -> new StationReading("11", name, 50.9, 15.3, List.of()))
                .withMessage("name must not be blank");
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsNonFiniteLatitude(double lat){
        assertThatIllegalArgumentException().isThrownBy(() -> new StationReading("11", "Czerniawa", lat, 15.3, List.of()))
                .withMessageContaining("lat must be finite");
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsNonFiniteLongitude(double lon){
        assertThatIllegalArgumentException().isThrownBy(() -> new StationReading("11", "Czerniawa", 50.9, lon, List.of()))
                .withMessageContaining("lon must be finite");
    }

    @Test
    void rejectsNullExternalId(){
        assertThatNullPointerException().isThrownBy(() -> new StationReading(null, "Czerniawa", 50.9, 15.3, List.of()))
                .withMessage("externalId");
    }

    @Test
    void rejectsNullName(){
        assertThatNullPointerException().isThrownBy(() -> new StationReading("11", null, 50.9, 15.3, List.of()))
                .withMessage("name");
    }

    @Test
    void rejectsNullReadings(){
        assertThatNullPointerException().isThrownBy(() -> new StationReading("11", "Czerniawa", 50.9, 15.3, null))
                .withMessage("readings");
    }

    @Test
    void rejectsNullElementInReadings(){
        List<ParameterReading> withNull = Arrays.asList(PM10, null);

        assertThatNullPointerException().isThrownBy(() -> new StationReading("11", "Czerniawa", 50.9, 15.3, withNull));
    }
}
