package pl.bartoszsredzinski.air_quality_map.source;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SourceFetchExceptionTest{

    @Test
    void keepsMessageAndOriginalCause(){
        RuntimeException cause = new RuntimeException("connection timed out");

        SourceFetchException exception = new SourceFetchException("GIOS station list unavailable", cause);

        assertThat(exception).hasMessage("GIOS station list unavailable");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void allowsMissingCause(){
        SourceFetchException exception = new SourceFetchException("GIOS returned an empty station list");

        assertThat(exception).hasMessage("GIOS returned an empty station list");
        assertThat(exception.getCause()).isNull();
    }

    // Wyjątek unchecked: warstwa ingest łapie go per źródło, a adapter nie zmusza reszty kodu do deklarowania throws.
    @Test
    void isUncheckedException(){
        assertThat(new SourceFetchException("x")).isInstanceOf(RuntimeException.class);
    }
}
