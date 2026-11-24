package pl.bartoszsredzinski.air_quality_map.source.gios;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class GiosClientConfigTest{

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(GiosClientConfig.class);

    @Test
    void createsNoBeansByDefault(){
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(RestClient.class);
            assertThat(context).doesNotHaveBean(RetryTemplate.class);
            assertThat(context).doesNotHaveBean(GiosProperties.class);
        });
    }

    @Test
    void createsNoBeansWhenExplicitlyDisabled(){
        runner.withPropertyValues("aqm.sources.gios.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RestClient.class);
                    assertThat(context).doesNotHaveBean(RetryTemplate.class);
                });
    }

    @Test
    void createsClientAndRetryTemplateWhenEnabled(){
        runner.withPropertyValues("aqm.sources.gios.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(GiosProperties.class);
                    assertThat(context).hasBean("giosRestClient");
                    assertThat(context).hasBean("giosRetryTemplate");
                });
    }

    // Wyłączone źródło nie waliduje swojej konfiguracji: błędna wartość nie blokuje startu aplikacji.
    @Test
    void disabledSourceDoesNotValidateItsConfiguration(){
        runner.withPropertyValues("aqm.sources.gios.enabled=false", "aqm.sources.gios.concurrency=0")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void failsToStartWhenEnabledWithInvalidConfiguration(){
        runner.withPropertyValues("aqm.sources.gios.enabled=true", "aqm.sources.gios.concurrency=0")
                .run(context -> assertThat(context).hasFailed());
    }
}
