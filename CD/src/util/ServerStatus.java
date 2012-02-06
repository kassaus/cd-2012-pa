package util;

public enum ServerStatus {
    OFLINE, ONLINE, INVALID;

    public static ServerStatus parse(final String text) {
        try {
            return valueOf(text);
        } catch (final Exception e) {
            return INVALID;
        }
    }
}
