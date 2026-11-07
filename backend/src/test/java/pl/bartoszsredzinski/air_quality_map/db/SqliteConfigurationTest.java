package pl.bartoszsredzinski.air_quality_map.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.zaxxer.hikari.HikariDataSource;

import pl.bartoszsredzinski.air_quality_map.support.SqliteIntegrationTest;

class SqliteConfigurationTest extends SqliteIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void usesWriteAheadLogging() {
        String journalMode = jdbc.sql("PRAGMA journal_mode").query(String.class).single();

        assertThat(journalMode).isEqualToIgnoringCase("wal");
    }

    @Test
    void appliesPragmasToEveryPooledConnection() throws SQLException {
        int poolSize = dataSource.unwrap(HikariDataSource.class).getMaximumPoolSize();
        List<Connection> connections = new ArrayList<>();
        try {
            // Borrowing all connections at once forces the pool to hand out distinct physical connections.
            for (int i = 0; i < poolSize; i++) {
                connections.add(dataSource.getConnection());
            }
            for (Connection connection : connections) {
                assertThat(pragma(connection, "foreign_keys")).as("foreign_keys").isEqualTo(1);
                assertThat(pragma(connection, "busy_timeout")).as("busy_timeout").isEqualTo(5000);
                assertThat(pragma(connection, "synchronous")).as("synchronous (1 = NORMAL)").isEqualTo(1);
            }
        } finally {
            for (Connection connection : connections) {
                connection.close();
            }
        }
    }

    /**
     * Documents the effect of {@code transaction_mode=IMMEDIATE}: a transaction takes the write lock at BEGIN,
     * even if it only reads. Hence a long read-only {@code @Transactional} would block writers.
     */
    @Test
    void transactionTakesWriteLockAtBegin() throws SQLException {
        try (Connection reader = dataSource.getConnection(); Connection writer = dataSource.getConnection()) {
            reader.setAutoCommit(false);
            try (Statement statement = reader.createStatement()) {
                statement.executeQuery("SELECT count(*) FROM station").close();
            }
            execute(writer, "PRAGMA busy_timeout = 100");
            try {
                assertThatThrownBy(() -> execute(writer, """
                        INSERT INTO station (source_id, external_id, name, lat, lon, source_type)
                        VALUES ('gios', 'lock', 'Lock test', 50.0, 19.0, 'REFERENCE')"""))
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("SQLITE_BUSY");
            } finally {
                reader.rollback();
                // pragmas live on the physical connection and survive its return to the pool
                execute(writer, "PRAGMA busy_timeout = 5000");
            }
        }
    }

    private static long pragma(Connection connection, String name) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA " + name)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
