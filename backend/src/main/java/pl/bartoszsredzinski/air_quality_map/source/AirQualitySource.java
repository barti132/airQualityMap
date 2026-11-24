package pl.bartoszsredzinski.air_quality_map.source;

import pl.bartoszsredzinski.air_quality_map.station.SourceType;

import java.util.List;

/**
 * Źródło danych o jakości powietrza: adapter jednego dostawcy (np. GIOŚ).
 * <p>
 * Adapter odpowiada wyłącznie za pobranie danych i zmapowanie ich do wspólnego modelu.
 * Zapis do bazy, deduplikację i obliczanie indeksu robi warstwa wyżej.
 */
public interface AirQualitySource{

    /** Stały identyfikator źródła, zapisywany jako {@code station.source_id} (np. {@code "gios"}). */
    String id();

    /** Typ źródła, czyli wiarygodność jego pomiarów: stacje oficjalne albo czujniki obywatelskie. */
    SourceType type();

    /**
     * Pobiera aktualne odczyty ze wszystkich stacji, które źródło aktualnie wymienia.
     * <ul>
     *   <li>Zwraca <b>każdą</b> stację z listy źródła, także z pustym {@code readings}, gdy pomiarów
     *   nie udało się pobrać. Dzięki temu warstwa wyżej może bezpiecznie uznać za nieaktywne stacje,
     *   których na liście brakuje (błąd pobierania pomiarów nie dezaktywuje stacji).</li>
     *   <li>Pojedyncze błędne stacje lub stanowiska pomija z wpisem w logu, bez wyjątku.</li>
     *   <li>Rzuca {@link SourceFetchException}, gdy nie da się pobrać nawet listy stacji.</li>
     * </ul>
     * Wywołanie blokuje i może trwać minutami (wiele zapytań HTTP), więc nie wolno go wykonywać
     * wewnątrz transakcji bazodanowej: SQLite dopuszcza jeden zapis naraz.
     *
     * @throws SourceFetchException gdy źródło jest całkowicie niedostępne
     */
    List<StationReading> fetch();
}
