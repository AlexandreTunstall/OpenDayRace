package uk.ac.exeter.opendayrace.common.world;

public class WorldParseException extends Exception {
    public WorldParseException() {
        super();
    }

    public WorldParseException(String message) {
        super(message);
    }

    public WorldParseException(Throwable cause) {
        super(cause);
    }

    public WorldParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
