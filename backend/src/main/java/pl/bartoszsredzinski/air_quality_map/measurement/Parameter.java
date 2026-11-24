package pl.bartoszsredzinski.air_quality_map.measurement;

/**
 * Mierzone zanieczyszczenie: parametry używane przez europejski indeks EAQI.
 * <p>
 * Nazwy stałych są zapisywane w bazie jako tekst ({@code measurement.parameter}), więc nie zmieniaj
 * ich nazw i nie opieraj się na {@code ordinal()}. Dodanie nowej stałej jest bezpieczne. Mapowanie
 * kodów konkretnego dostawcy (np. {@code "PM2.5"} na {@link #PM25}) należy do jego adaptera.
 */
public enum Parameter{
    PM25,
    PM10,
    NO2,
    O3,
    SO2
}
