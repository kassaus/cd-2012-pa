package util;

public enum ServerCommands {
    HALT, RESTART, EXIT, HELP, INVALID;

    public static ServerCommands parse(final String text) {
        try {
            return valueOf(text);
        } catch (final Exception e) {
            return INVALID;
        }
    }
}
