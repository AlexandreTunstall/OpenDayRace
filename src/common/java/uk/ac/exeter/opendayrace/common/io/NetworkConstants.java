package uk.ac.exeter.opendayrace.common.io;

public class NetworkConstants {
    public static final byte VERSION = 1;
    public static final byte VERSION_END = 0;

    // Success statuses (sign bit 0)
    public static final byte STATUS_OK = 0;
    public static final byte STATUS_AWAITING_SELECTION = 1;
    public static final byte STATUS_SHOW_PATHS = 2;

    // Failure statuses (sign bit 1)
    public static final byte STATUS_INCOMPATIBLE_VERSION = -1;
    public static final byte STATUS_INVALID_SELECTION = -2;

    public static final byte PATH_LEFT_LEFT = 5;
    public static final byte PATH_LEFT_RIGHT = 6;
    public static final byte PATH_RIGHT_LEFT = 7;
    public static final byte PATH_RIGHT_RIGHT = 8;

    public static final byte PLAYER_PATH_COUNT_LEFT_1 = 9;
    public static final byte PLAYER_PATH_COUNT_RIGHT_1 = 10;
    public static final byte PLAYER_PATH_COUNT_LEFT_2 = 11;
    public static final byte PLAYER_PATH_COUNT_RIGHT_2 = 12;

    // Prevent instantiation
    private NetworkConstants() {}
}
