package pl.bartoszsredzinski.air_quality_map.source.gios;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GiosRetryTest{

    private static final String PATH = "/pjp-api/v1/rest/data";
    private static final String SCENARIO = "flaky";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final GiosProperties properties = properties();

    private GiosProperties properties(String... overrides){
        String[] defaults = {"retry.max-retries", "2", "retry.initial-delay", "10ms", "retry.multiplier", "2.0", "read-timeout", "400ms"};
        String[] all = new String[defaults.length + overrides.length];
        System.arraycopy(defaults, 0, all, 0, defaults.length);
        System.arraycopy(overrides, 0, all, defaults.length, overrides.length);

        return GiosTestProperties.bind(wireMock.baseUrl() + "/pjp-api/v1/rest", all);
    }

    private String fetch(GiosProperties properties) throws RetryException{
        GiosClientConfig config = new GiosClientConfig();
        RestClient client = config.giosRestClient(properties);
        RetryTemplate retry = config.giosRetryTemplate(properties);

        return retry.execute(() -> client.get()
                .uri("/data")
                .retrieve()
                .body(String.class));
    }

    private void verifyRequests(int expected){
        wireMock.verify(expected, getRequestedFor(urlEqualTo(PATH)));
    }

    @Test
    void succeedsWithoutRetryWhenFirstAttemptWorks() throws RetryException{
        wireMock.stubFor(get(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200)
                .withBody("ok")));

        assertThat(fetch(properties)).isEqualTo("ok");
        verifyRequests(1);
    }

    @Test
    void retriesAfterServerError() throws RetryException{
        wireMock.stubFor(get(urlEqualTo(PATH)).inScenario(SCENARIO)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("recovered"));
        wireMock.stubFor(get(urlEqualTo(PATH)).inScenario(SCENARIO)
                .whenScenarioStateIs("recovered")
                .willReturn(aResponse().withStatus(200)
                        .withBody("ok")));

        assertThat(fetch(properties)).isEqualTo("ok");
        verifyRequests(2);
    }

    @Test
    void retriesAfterTooManyRequests() throws RetryException{
        wireMock.stubFor(get(urlEqualTo(PATH)).inScenario(SCENARIO)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(429))
                .willSetStateTo("recovered"));
        wireMock.stubFor(get(urlEqualTo(PATH)).inScenario(SCENARIO)
                .whenScenarioStateIs("recovered")
                .willReturn(aResponse().withStatus(200)
                        .withBody("ok")));

        assertThat(fetch(properties)).isEqualTo("ok");
        verifyRequests(2);
    }

    @Test
    void retriesAfterReadTimeout() throws RetryException{
        wireMock.stubFor(get(urlEqualTo(PATH)).inScenario(SCENARIO)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(200)
                        .withBody("late")
                        .withFixedDelay(1500))
                .willSetStateTo("recovered"));
        wireMock.stubFor(get(urlEqualTo(PATH)).inScenario(SCENARIO)
                .whenScenarioStateIs("recovered")
                .willReturn(aResponse().withStatus(200)
                        .withBody("ok")));

        assertThat(fetch(properties)).isEqualTo("ok");
        verifyRequests(2);
    }

    @Test
    void givesUpAfterMaxRetries(){
        wireMock.stubFor(get(urlEqualTo(PATH)).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> fetch(properties)).isInstanceOf(RetryException.class)
                .hasCauseInstanceOf(HttpServerErrorException.class);
        verifyRequests(3);
    }

    @Test
    void doesNotRetryWhenMaxRetriesIsZero(){
        wireMock.stubFor(get(urlEqualTo(PATH)).willReturn(aResponse().withStatus(503)));
        GiosProperties noRetries = properties("retry.max-retries", "0");

        assertThatThrownBy(() -> fetch(noRetries)).isInstanceOf(RetryException.class);
        verifyRequests(1);
    }

    // Opóźnienie rośnie wykładniczo: 100 ms przed pierwszym ponowieniem i 200 ms przed drugim (razem 300 ms).
    // Sprawdzamy tylko dolną granicę: uśpienie nigdy nie jest krótsze, a górna granica byłaby niestabilna.
    @Test
    void backsOffExponentiallyBetweenRetries(){
        wireMock.stubFor(get(urlEqualTo(PATH)).willReturn(aResponse().withStatus(503)));
        GiosProperties slowBackoff = properties("retry.initial-delay", "100ms");

        long start = System.nanoTime();
        assertThatThrownBy(() -> fetch(slowBackoff)).isInstanceOf(RetryException.class);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofMillis(280));
    }

    @Test
    void doesNotRetryClientErrors(){
        wireMock.stubFor(get(urlEqualTo(PATH)).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> fetch(properties)).isInstanceOf(RetryException.class)
                .hasCauseInstanceOf(HttpClientErrorException.NotFound.class);
        verifyRequests(1);
    }
}
