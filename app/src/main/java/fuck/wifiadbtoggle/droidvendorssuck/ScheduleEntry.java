package fuck.wifiadbtoggle.droidvendorssuck;

public final class ScheduleEntry {
    public final long id;
    public final String title;
    public final long startMillis;
    public final long endMillis;
    public final ScheduleMode mode;
    public final boolean enabled;

    public ScheduleEntry(long id, String title, long startMillis, long endMillis, ScheduleMode mode, boolean enabled) {
        this.id = id;
        this.title = title;
        this.startMillis = startMillis;
        this.endMillis = endMillis;
        this.mode = mode;
        this.enabled = enabled;
    }
}
