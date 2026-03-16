package fuck.wifiadbtoggle.droidvendorssuck;

public enum ScheduleMode {
    FORCE_ON("force_on"),
    FORCE_OFF("force_off"),
    RESPECT("respect");

    public final String id;

    ScheduleMode(String id) {
        this.id = id;
    }

    public static ScheduleMode fromId(String id) {
        for (ScheduleMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return RESPECT;
    }
}
