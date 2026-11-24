package pl.bartoszsredzinski.air_quality_map.source.gios;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GiosHttpClientTest{

    private static final String API_PREFIX = "/pjp-api/v1/rest";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StationDto(@JsonProperty("Identyfikator stacji") long id, @JsonProperty("Nazwa stacji") String name,
                      @JsonProperty("WGS84 φ N") String lat, @JsonProperty("WGS84 λ E") String lon){
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StationsPage(@JsonProperty("Lista stacji pomiarowych") List<StationDto> stations,
                        @JsonProperty("totalPages") int totalPages){
    }

    private RestClient client(String... overrides){
        GiosProperties properties = GiosTestProperties.bind(wireMock.baseUrl() + API_PREFIX, overrides);
        return new GiosClientConfig().giosRestClient(properties);
    }

    @Test
    void readsJsonLdResponseWithPolishPropertyNames() throws IOException{
        String body = new ClassPathResource("gios/stations-page-0.json").getContentAsString(StandardCharsets.UTF_8);
        wireMock.stubFor(get(urlEqualTo(API_PREFIX + "/station/findAll?page=0&size=2")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/ld+json;charset=utf-8")
                .withBody(body)));

        StationsPage page = client().get()
                .uri("/station/findAll?page={page}&size={size}", 0, 2)
                .retrieve()
                .body(StationsPage.class);

        assertThat(page).isNotNull();
        assertThat(page.totalPages()).isEqualTo(144);
        assertThat(page.stations()).hasSize(2);
        assertThat(page.stations()
                .get(0)).isEqualTo(new StationDto(11, "Czerniawa", "50.912475", "15.312190"));
        assertThat(page.stations()
                .get(1)
                .name()).isEqualTo("Dzierżoniów, ul. Piłsudskiego");
    }

    @Test
    void sendsConfiguredUserAgent(){
        wireMock.stubFor(get(urlEqualTo(API_PREFIX + "/ping")).willReturn(aResponse().withStatus(200)
                .withBody("pong")));

        client().get()
                .uri("/ping")
                .retrieve()
                .body(String.class);

        wireMock.verify(getRequestedFor(urlEqualTo(API_PREFIX + "/ping")).withHeader("User-Agent", equalTo("air-quality-map")));
    }

    @Test
    void failsWithResourceAccessExceptionWhenReadTimeoutIsExceeded(){
        wireMock.stubFor(get(urlEqualTo(API_PREFIX + "/slow")).willReturn(aResponse().withStatus(200)
                .withBody("late")
                .withFixedDelay(2000)));
        RestClient client = client("read-timeout", "300ms");

        assertThatThrownBy(() -> client.get()
                .uri("/slow")
                .retrieve()
                .body(String.class)).isInstanceOf(ResourceAccessException.class);
    }
}
