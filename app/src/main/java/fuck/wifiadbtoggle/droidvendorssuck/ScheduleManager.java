package fuck.wifiadbtoggle.droidvendorssuck;

import android.content.Context;
import java.util.Calendar;
import java.util.List;

public final class ScheduleManager {
    private ScheduleManager() {
    }

    public static ScheduleMode getActiveMode(Context context) {
        return getActiveMode(context, System.currentTimeMillis());
    }

    public static ScheduleMode getActiveMode(Context context, long now) {
        if (!Settings.isScheduleEnabled(context)) return null;
        List<ScheduleEntry> entries = new ScheduleDbHelper(context).listAll();
        ScheduleMode result = null;
        for (ScheduleEntry entry : entries) {
            if (!entry.enabled) continue;
            if (entry.startMillis <= now && entry.endMillis > now) {
                if (entry.mode == ScheduleMode.FORCE_OFF) {
                    return ScheduleMode.FORCE_OFF;
                }
                if (entry.mode == ScheduleMode.FORCE_ON) {
                    result = ScheduleMode.FORCE_ON;
                } else if (result == null) {
                    result = ScheduleMode.RESPECT;
                }
            }
        }
        return result;
    }

    public static void applyScheduleNow(Context context) {
        ScheduleMode mode = getActiveMode(context);
        if (mode == null) return;
        if (!ShellRunner.canUseRoot()) return;
        if (mode == ScheduleMode.FORCE_ON) {
            AdbWifiController.enable(context);
        } else if (mode == ScheduleMode.FORCE_OFF) {
            AdbWifiController.disable(context);
        }
    }

    public static Long getNextBoundary(Context context) {
        return getNextBoundary(context, System.currentTimeMillis());
    }

    public static Long getNextBoundary(Context context, long now) {
        List<ScheduleEntry> entries = new ScheduleDbHelper(context).listAll();
        Long next = null;
        for (ScheduleEntry entry : entries) {
            if (!entry.enabled) continue;
            if (entry.startMillis > now) next = minMillis(next, entry.startMillis);
            if (entry.endMillis > now) next = minMillis(next, entry.endMillis);
        }
        return next;
    }

    public static DayBounds getDayBounds(long dayMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dayMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        long end = cal.getTimeInMillis();
        return new DayBounds(start, end);
    }

    private static long minMillis(Long a, long b) {
        return a == null || b < a ? b : a;
    }

    public static final class DayBounds {
        public final long startMillis;
        public final long endMillis;

        public DayBounds(long startMillis, long endMillis) {
            this.startMillis = startMillis;
            this.endMillis = endMillis;
        }
    }
}
