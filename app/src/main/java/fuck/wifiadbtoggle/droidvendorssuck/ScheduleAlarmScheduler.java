package fuck.wifiadbtoggle.droidvendorssuck;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class ScheduleAlarmScheduler {
    private ScheduleAlarmScheduler() {
    }

    public static void scheduleNext(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ScheduleAlarmReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            pendingFlags()
        );

        if (!Settings.isScheduleEnabled(context) || !new ScheduleDbHelper(context).hasAnyEnabled()) {
            alarmManager.cancel(pending);
            return;
        }

        Long next = ScheduleManager.getNextBoundary(context);
        if (next == null) {
            alarmManager.cancel(pending);
            return;
        }
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, next, pending);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, next, pending);
        }
    }

    private static int pendingFlags() {
        if (Build.VERSION.SDK_INT >= 23) {
            return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.FLAG_UPDATE_CURRENT;
    }
}
