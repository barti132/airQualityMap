package pl.bartoszsredzinski.air_quality_map.source;

import pl.bartoszsredzinski.air_quality_map.measurement.Parameter;

import java.time.Instant;
import java.util.Objects;

public record ParameterReading(Parameter parameter, double value, String unit, Instant measuredAt){

    public ParameterReading{
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(measuredAt, "measuredAt");

        if(unit.isBlank()){
            throw new IllegalArgumentException("unit must not be blank");
        }
        if(!Double.isFinite(value)){
            throw new IllegalArgumentException("value must be finite: " + value);
        }
    }
}
