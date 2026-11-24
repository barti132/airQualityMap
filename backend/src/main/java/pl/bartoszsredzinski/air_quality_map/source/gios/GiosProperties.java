package pl.bartoszsredzinski.air_quality_map.source.gios;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Konfiguracja adaptera GIOŚ ({@code aqm.sources.gios.*}). Wartości domyślne są zdefiniowane tutaj,
 * w jednym miejscu; {@code application.yaml} włącza tylko źródło.
 *
 * @param enabled         czy źródło jest włączone (wyłączone nie tworzy żadnych beanów)
 * @param baseUrl         adres bazowy API v1, bez końcowego ukośnika
 * @param connectTimeout  limit czasu nawiązania połączenia
 * @param readTimeout     limit czasu oczekiwania na odpowiedź
 * @param concurrency     liczba równoległych zapytań o pomiary (API ma limity zapytań na minutę)
 * @param stationPageSize rozmiar strony przy pobieraniu listy stacji
 * @param sensorCacheTtl  jak długo trzymać w pamięci listę stanowisk stacji (zmienia się rzadko)
 * @param retry           ponawianie nieudanych zapytań
 */
@Validated
@ConfigurationProperties("aqm.sources.gios")
public record GiosProperties(@DefaultValue("false") boolean enabled,
                             @DefaultValue("https://api.gios.gov.pl/pjp-api/v1/rest") @NotBlank String baseUrl,
                             @DefaultValue("5s") @NotNull Duration connectTimeout,
                             @DefaultValue("15s") @NotNull Duration readTimeout, @DefaultValue("4") @Min(1) int concurrency,
                             @DefaultValue("1000") @Min(1) int stationPageSize,
                             @DefaultValue("24h") @NotNull Duration sensorCacheTtl, @DefaultValue @Valid Retry retry){

    /**
     * @param maxRetries   ile razy ponowić po pierwszej nieudanej próbie (0 wyłącza ponawianie)
     * @param initialDelay opóźnienie przed pierwszą ponowną próbą
     * @param multiplier   mnożnik opóźnienia przy kolejnych próbach (exponential backoff)
     */
    public record Retry(@DefaultValue("3") @Min(0) int maxRetries, @DefaultValue("500ms") @NotNull Duration initialDelay,
                        @DefaultValue("2.0") @DecimalMin("1.0") double multiplier){
    }

    @AssertTrue(message = "connectTimeout and readTimeout must be positive")
    boolean isTimeoutsPositive(){
        return isPositive(connectTimeout) && isPositive(readTimeout);
    }

    private static boolean isPositive(Duration duration){
        return duration != null && !duration.isZero() && !duration.isNegative();
    }
}
