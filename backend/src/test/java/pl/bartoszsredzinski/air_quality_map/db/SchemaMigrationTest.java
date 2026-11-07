package pl.bartoszsredzinski.air_quality_map.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pl.bartoszsredzinski.air_quality_map.support.SqliteIntegrationTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchemaMigrationTest extends SqliteIntegrationTest{

    @Test
    void flywayAppliedInitialMigration(){
        List<String> versions = jdbc.sql("""
                        SELECT version FROM flyway_schema_history WHERE success = 1 AND version IS NOT NULL""")
                .query(String.class)
                .list();
        List<String> tables = jdbc.sql("SELECT name FROM sqlite_master WHERE type = 'table'")
                .query(String.class)
                .list();

        assertThat(versions).contains("1");
        assertThat(tables).contains("station", "measurement");
    }

    @Test
    void rejectsSecondStationWithTheSameSourceAndExternalId(){
        insertStation("gios", "1", "REFERENCE", 50.06, 19.94, 1);

        assertThatThrownBy(() -> insertStation("gios", "1", "REFERENCE", 50.06, 19.94, 1)).rootCause()
                .hasMessageContaining("UNIQUE constraint failed");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"unknown source type, OTHER, 50.0, 19.0, 1", "latitude above 90, REFERENCE, 100.0, 19.0, 1", "longitude above 180, REFERENCE, 50.0, 200.0, 1", "active flag other than 0 or 1, REFERENCE, 50.0, 19.0, 2"})
    void rejectsInvalidStation(String description, String sourceType, double lat, double lon, int active){
        assertThatThrownBy(() -> insertStation("gios", "bad", sourceType, lat, lon, active)).rootCause()
                .hasMessageContaining("CHECK constraint failed");
    }

    @Test
    void rejectsMeasurementForUnknownStation(){
        assertThatThrownBy(() -> insertMeasurement(999L, "PM10", 10.0, 1000L)).rootCause()
                .hasMessageContaining("FOREIGN KEY constraint failed");
    }

    @Test
    void rejectsDuplicateMeasurement(){
        long stationId = createStation();
        insertMeasurement(stationId, "PM10", 10.0, 1000L);

        assertThatThrownBy(() -> insertMeasurement(stationId, "PM10", 20.0, 1000L)).rootCause()
                .hasMessageContaining("UNIQUE constraint failed");
    }

    @Test
    void rejectsTextInNumericColumn(){
        long stationId = createStation();

        assertThatThrownBy(() -> jdbc.sql("""
                        INSERT INTO measurement (station_id, parameter, value, unit, measured_at, fetched_at)
                        VALUES (?, 'PM10', ?, 'µg/m³', 1000, 1000)""")
                .params(stationId, "abc")
                .update()).rootCause()
                .hasMessageContaining("cannot store TEXT value in REAL column");
    }

    @Test
    void upsertReplacesValueOfTheSameMeasurement(){
        long stationId = createStation();

        upsertMeasurement(stationId, "PM10", 10.0, 1000L);
        upsertMeasurement(stationId, "PM10", 20.0, 1000L);

        assertThat(jdbc.sql("SELECT count(*) FROM measurement")
                .query(Long.class)
                .single()).isEqualTo(1L);
        assertThat(jdbc.sql("SELECT value FROM measurement")
                .query(Double.class)
                .single()).isEqualTo(20.0);
    }

    private long createStation(){
        insertStation("gios", "1", "REFERENCE", 50.06, 19.94, 1);
        return jdbc.sql("SELECT id FROM station WHERE source_id = 'gios' AND external_id = '1'")
                .query(Long.class)
                .single();
    }

    private void insertStation(String sourceId, String externalId, String sourceType, double lat, double lon, int active){
        jdbc.sql("""
                        INSERT INTO station (source_id, external_id, name, lat, lon, source_type, active)
                        VALUES (?, ?, 'Test station', ?, ?, ?, ?)""")
                .params(sourceId, externalId, lat, lon, sourceType, active)
                .update();
    }

    private void insertMeasurement(long stationId, String parameter, double value, long measuredAt){
        jdbc.sql("""
                        INSERT INTO measurement (station_id, parameter, value, unit, measured_at, fetched_at)
                        VALUES (?, ?, ?, 'µg/m³', ?, ?)""")
                .params(stationId, parameter, value, measuredAt, measuredAt)
                .update();
    }

    private void upsertMeasurement(long stationId, String parameter, double value, long measuredAt){
        jdbc.sql("""
                        INSERT INTO measurement (station_id, parameter, value, unit, measured_at, fetched_at)
                        VALUES (?, ?, ?, 'µg/m³', ?, ?)
                        ON CONFLICT (station_id, parameter, measured_at)
                        DO UPDATE SET value = excluded.value, fetched_at = excluded.fetched_at""")
                .params(stationId, parameter, value, measuredAt, measuredAt)
                .update();
    }
}
