package fuck.wifiadbtoggle.droidvendorssuck

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ScheduleAlarmScheduler {
    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ScheduleAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            pendingFlags()
        )

        val next = ScheduleManager.getNextBoundary(context)
        if (next == null) {
            alarmManager.cancel(pending)
            return
        }
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, next, pending)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, next, pending)
        }
    }

    private fun pendingFlags(): Int {
        return if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }
}
