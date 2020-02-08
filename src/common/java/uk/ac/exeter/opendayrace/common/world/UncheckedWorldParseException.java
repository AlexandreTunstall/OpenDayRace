package uk.ac.exeter.opendayrace.common.world;

/**
 * Because throwing checked exceptions inside streams is a nightmare
 */
class UncheckedWorldParseException extends RuntimeException {
    private final WorldParseException e;

    public UncheckedWorldParseException(WorldParseException e) {
        super(e);
        this.e = e;
    }

    public WorldParseException getChecked() {
        return e;
    }
}
