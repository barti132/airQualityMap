package pl.bartoszsredzinski.air_quality_map.station;

/**
 * Typ źródła danych; od niego zależy wiarygodność pomiaru pokazywana w interfejsie.
 * <p>
 * Nazwy stałych są zapisywane w bazie jako tekst ({@code station.source_type}, z ograniczeniem CHECK),
 * więc nie zmieniaj ich nazw i nie opieraj się na {@code ordinal()}. Dodanie nowej stałej wymaga
 * też zmiany ograniczenia w migracji.
 */
public enum SourceType{
    REFERENCE,
    LOW_COST
}
