package pl.bartoszsredzinski.air_quality_map.source.gios;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Buduje {@link GiosProperties} tym samym mechanizmem co aplikacja (Binder), więc testy widzą też
 * wartości domyślne z {@code @DefaultValue}.
 */
final class GiosTestProperties{

    private static final String PREFIX = "aqm.sources.gios";

    private GiosTestProperties(){
    }

    /**
     * @param overrides pary klucz i wartość względem prefiksu {@code aqm.sources.gios},
     *                  np. {@code "retry.max-retries", "2"}
     */
    static GiosProperties bind(String baseUrl, String... overrides){
        Map<String, String> values = new HashMap<>();
        values.put(PREFIX + ".base-url", baseUrl);
        for(int i = 0; i < overrides.length; i += 2){
            values.put(PREFIX + "." + overrides[i], overrides[i + 1]);
        }

        return new Binder(new MapConfigurationPropertySource(values)).bind(PREFIX, Bindable.of(GiosProperties.class))
                .get();
    }
}
