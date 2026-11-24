package pl.bartoszsredzinski.air_quality_map.source.gios;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "aqm.sources.gios", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(GiosProperties.class)
class GiosClientConfig{

    static final String USER_AGENT = "air-quality-map";

    @Bean
    RestClient giosRestClient(GiosProperties properties){
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
    }

    @Bean
    RetryTemplate giosRetryTemplate(GiosProperties properties){
        GiosProperties.Retry retry = properties.retry();
        RetryPolicy policy = RetryPolicy.builder()
                .maxRetries(retry.maxRetries())
                .delay(retry.initialDelay())
                .multiplier(retry.multiplier())
                .predicate(GiosClientConfig::isRetryable)
                .build();

        return new RetryTemplate(policy);
    }

    static boolean isRetryable(Throwable throwable){
        return throwable instanceof ResourceAccessException
                || throwable instanceof HttpServerErrorException
                || throwable instanceof HttpClientErrorException.TooManyRequests;
    }
}
