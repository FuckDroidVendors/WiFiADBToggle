package fuck.wifiadbtoggle.droidvendorssuck

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (Settings.isAutoStartEnabled(context) && Settings.isAnyMonitorRuleEnabled(context)) {
            NetworkMonitorService.start(context)
        }
        if (Settings.isMediaButtonsEnabled(context)) {
            MediaButtonService.start(context)
        }
        if (Settings.isPersistentNotificationEnabled(context) &&
            !(Settings.isAutoStartEnabled(context) && Settings.isAnyMonitorRuleEnabled(context))
        ) {
            QuickControlService.start(context)
        }
        if (Settings.isScheduleEnabled(context)) {
            ScheduleAlarmScheduler.scheduleNext(context)
        }
    }
}
