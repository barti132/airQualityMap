package pl.bartoszsredzinski.air_quality_map.source;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.bartoszsredzinski.air_quality_map.measurement.Parameter;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ParameterReadingTest{

    private static final String UNIT = "µg/m³";
    private static final Instant MEASURED_AT = Instant.parse("2026-11-18T19:00:00Z");

    @Test
    void keepsGivenValues(){
        ParameterReading reading = new ParameterReading(Parameter.PM10, 12.5, UNIT, MEASURED_AT);

        assertThat(reading.parameter()).isEqualTo(Parameter.PM10);
        assertThat(reading.value()).isEqualTo(12.5);
        assertThat(reading.unit()).isEqualTo(UNIT);
        assertThat(reading.measuredAt()).isEqualTo(MEASURED_AT);
    }

    // Czujniki zgłaszają czasem małe wartości ujemne (np. NO), a zakresy to polityka filtra z warstwy ingest,
    // nie modelu: rekord ma przyjąć każdą skończoną liczbę.
    @ParameterizedTest
    @ValueSource(doubles = {-0.4, 0.0, 1_000_000.0})
    void acceptsAnyFiniteValue(double value){
        ParameterReading reading = new ParameterReading(Parameter.NO2, value, UNIT, MEASURED_AT);

        assertThat(reading.value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsNonFiniteValue(double value){
        assertThatIllegalArgumentException().isThrownBy(() -> new ParameterReading(Parameter.O3, value, UNIT, MEASURED_AT))
                .withMessageContaining("value must be finite");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void rejectsBlankUnit(String unit){
        assertThatIllegalArgumentException().isThrownBy(() -> new ParameterReading(Parameter.SO2, 1.0, unit, MEASURED_AT))
                .withMessage("unit must not be blank");
    }

    @Test
    void rejectsNullParameter(){
        assertThatNullPointerException().isThrownBy(() -> new ParameterReading(null, 1.0, UNIT, MEASURED_AT))
                .withMessage("parameter");
    }

    @Test
    void rejectsNullUnit(){
        assertThatNullPointerException().isThrownBy(() -> new ParameterReading(Parameter.PM25, 1.0, null, MEASURED_AT))
                .withMessage("unit");
    }

    @Test
    void rejectsNullMeasuredAt(){
        assertThatNullPointerException().isThrownBy(() -> new ParameterReading(Parameter.PM25, 1.0, UNIT, null))
                .withMessage("measuredAt");
    }
}
