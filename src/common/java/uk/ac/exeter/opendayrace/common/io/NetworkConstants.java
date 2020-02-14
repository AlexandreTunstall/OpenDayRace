package uk.ac.exeter.opendayrace.common.io;

public class NetworkConstants {
    public static final byte VERSION = 1;
    public static final byte VERSION_END = 0;

    // Success statuses (sign bit 0)
    public static final byte STATUS_OK = 0;
    public static final byte STATUS_AWAITING_SELECTION = 1;
    public static final byte STATUS_SHOW_PATHS = 2;
    public static final byte STATUS_PATH_TIME = 3;

    // Failure statuses (sign bit 1)
    public static final byte STATUS_INCOMPATIBLE_VERSION = -1;
    public static final byte STATUS_INVALID_SELECTION = -2;

    public static final byte PATH_LEFT_LEFT = 5;
    public static final byte PATH_LEFT_RIGHT = 6;
    public static final byte PATH_RIGHT_LEFT = 7;
    public static final byte PATH_RIGHT_RIGHT = 8;

    // Prevent instantiation
    private NetworkConstants() {}
}
