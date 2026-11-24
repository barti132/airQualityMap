package pl.bartoszsredzinski.air_quality_map.source.gios;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class GiosPropertiesTest{

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class);

    @EnableConfigurationProperties(GiosProperties.class)
    static class PropertiesConfig{
    }

    @Test
    void isDisabledByDefault(){
        runner.run(context -> assertThat(context.getBean(GiosProperties.class)
                .enabled()).isFalse());
    }

    @Test
    void hasSensibleDefaults(){
        runner.withPropertyValues("aqm.sources.gios.enabled=true")
                .run(context -> {
                    GiosProperties properties = context.getBean(GiosProperties.class);

                    assertThat(properties.enabled()).isTrue();
                    assertThat(properties.baseUrl()).isEqualTo("https://api.gios.gov.pl/pjp-api/v1/rest");
                    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
                    assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(15));
                    assertThat(properties.concurrency()).isEqualTo(4);
                    assertThat(properties.stationPageSize()).isEqualTo(1000);
                    assertThat(properties.sensorCacheTtl()).isEqualTo(Duration.ofHours(24));
                    assertThat(properties.retry()
                            .maxRetries()).isEqualTo(3);
                    assertThat(properties.retry()
                            .initialDelay()).isEqualTo(Duration.ofMillis(500));
                    assertThat(properties.retry()
                            .multiplier()).isEqualTo(2.0);
                });
    }

    @Test
    void bindsOverriddenValues(){
        runner.withPropertyValues("aqm.sources.gios.base-url=http://localhost:9999/api", "aqm.sources.gios.connect-timeout=2s",
                        "aqm.sources.gios.read-timeout=750ms", "aqm.sources.gios.concurrency=8", "aqm.sources.gios.station-page-size=500",
                        "aqm.sources.gios.sensor-cache-ttl=6h", "aqm.sources.gios.retry.max-retries=5",
                        "aqm.sources.gios.retry.initial-delay=100ms", "aqm.sources.gios.retry.multiplier=1.5")
                .run(context -> {
                    GiosProperties properties = context.getBean(GiosProperties.class);

                    assertThat(properties.baseUrl()).isEqualTo("http://localhost:9999/api");
                    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
                    assertThat(properties.readTimeout()).isEqualTo(Duration.ofMillis(750));
                    assertThat(properties.concurrency()).isEqualTo(8);
                    assertThat(properties.stationPageSize()).isEqualTo(500);
                    assertThat(properties.sensorCacheTtl()).isEqualTo(Duration.ofHours(6));
                    assertThat(properties.retry()
                            .maxRetries()).isEqualTo(5);
                    assertThat(properties.retry()
                            .initialDelay()).isEqualTo(Duration.ofMillis(100));
                    assertThat(properties.retry()
                            .multiplier()).isEqualTo(1.5);
                });
    }

    @ParameterizedTest(name = "{0}={1} is rejected")
    @CsvSource({"concurrency, 0, concurrency", "station-page-size, 0, stationPageSize", "retry.max-retries, -1, maxRetries", "retry.multiplier, 0.5, multiplier", "connect-timeout, 0s, must be positive", "read-timeout, -1s, must be positive"})
    void rejectsInvalidValues(String property, String value, String expectedInMessage){
        runner.withPropertyValues("aqm.sources.gios." + property + "=" + value)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining(expectedInMessage);
                });
    }
}
