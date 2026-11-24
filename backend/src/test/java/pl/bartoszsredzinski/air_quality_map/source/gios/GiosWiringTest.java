package pl.bartoszsredzinski.air_quality_map.source.gios;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import pl.bartoszsredzinski.air_quality_map.support.SqliteIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pełny kontekst aplikacji z włączonym źródłem: sprawdza, że konfiguracja GIOŚ jest znajdowana przez
 * skanowanie komponentów (testy z ApplicationContextRunner ładują ją ręcznie). Wyłączone jest domyślne
 * w profilu {@code test}, więc inne testy kontekstowe nie tworzą tych beanów.
 */
@TestPropertySource(properties = "aqm.sources.gios.enabled=true")
class GiosWiringTest extends SqliteIntegrationTest{

    @Autowired
    private ApplicationContext context;

    @Test
    void createsGiosBeansInFullApplicationContext(){
        assertThat(context.getBean("giosRestClient")).isInstanceOf(RestClient.class);
        assertThat(context.getBean("giosRetryTemplate")).isInstanceOf(RetryTemplate.class);
        assertThat(context.getBean(GiosProperties.class)
                .enabled()).isTrue();
    }
}
