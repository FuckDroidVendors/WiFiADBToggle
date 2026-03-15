package fuck.wifiadbtoggle.droidvendorssuck

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ScheduleManager.applyScheduleNow(context)
        ScheduleAlarmScheduler.scheduleNext(context)
    }
}
