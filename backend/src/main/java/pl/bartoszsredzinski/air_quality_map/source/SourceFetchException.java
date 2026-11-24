package pl.bartoszsredzinski.air_quality_map.source;

public class SourceFetchException extends RuntimeException{

    public SourceFetchException(String message){
        super(message);
    }

    public SourceFetchException(String message, Throwable cause){
        super(message, cause);
    }
}
