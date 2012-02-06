package util;

public enum FrontEndCommands {
    GET_TABLE, SEND_LOAD, LIST_TABLE, TIME, EXIT, INVALID;

    public static FrontEndCommands parse(final String text) {
        try {
            return valueOf(text);
        } catch (final Exception e) {
            return INVALID;
        }
    }
}
