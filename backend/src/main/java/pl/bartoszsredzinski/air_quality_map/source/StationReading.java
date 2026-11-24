package pl.bartoszsredzinski.air_quality_map.source;

import java.util.List;
import java.util.Objects;

public record StationReading(String externalId, String name, double lat, double lon, List<ParameterReading> readings){

    public StationReading{
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(readings, "readings");

        if(externalId.isBlank()){
            throw new IllegalArgumentException("externalId must not be blank");
        }
        if(name.isBlank()){
            throw new IllegalArgumentException("name must not be blank");
        }
        if(!Double.isFinite(lat)){
            throw new IllegalArgumentException("lat must be finite: " + lat);
        }
        if(!Double.isFinite(lon)){
            throw new IllegalArgumentException("lon must be finite: " + lon);
        }

        // Kopia defensywna: rekord nie zmienia się, gdy ktoś zmodyfikuje listę przekazaną do konstruktora
        // (List.copyOf rzuci też NullPointerException dla null wewnątrz listy).
        readings = List.copyOf(readings);
    }
}
