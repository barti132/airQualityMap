package pl.bartoszsredzinski.air_quality_map.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


@SpringBootTest
@ActiveProfiles("test")
public abstract class SqliteIntegrationTest {

    private static final Path DB_DIR = createTempDbDir();

    @Autowired
    protected JdbcClient jdbc;

    @DynamicPropertySource
    static void sqliteProperties(DynamicPropertyRegistry registry) {
        registry.add("AQM_DB_PATH", () -> DB_DIR.resolve("test.db").toString());
    }

    @BeforeEach
    void cleanDatabase() {
        jdbc.sql("DELETE FROM measurement").update();
        jdbc.sql("DELETE FROM station").update();
    }

    private static Path createTempDbDir() {
        try {
            Path dir = Files.createTempDirectory("aqm-test-db-");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteQuietly(dir)));
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Best effort: on Windows the file may still be locked by the closing context.
    private static void deleteQuietly(Path dir) {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException ignored) {
            // leftover temp files are harmless
        }
    }
}
