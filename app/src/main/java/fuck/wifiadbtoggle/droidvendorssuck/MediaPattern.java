package fuck.wifiadbtoggle.droidvendorssuck;

public enum MediaPattern {
    SINGLE("single"),
    DOUBLE("double"),
    TRIPLE("triple"),
    LONG("long");

    public final String id;

    MediaPattern(String id) {
        this.id = id;
    }

    public static MediaPattern fromId(String id) {
        for (MediaPattern pattern : values()) {
            if (pattern.id.equals(id)) {
                return pattern;
            }
        }
        return DOUBLE;
    }
}
