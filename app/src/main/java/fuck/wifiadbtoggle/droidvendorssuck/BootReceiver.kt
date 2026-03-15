package fuck.wifiadbtoggle.droidvendorssuck

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (BuildConfig.FEATURE_MONITOR &&
            Settings.isAutoStartEnabled(context) &&
            Settings.isAnyMonitorRuleEnabled(context)
        ) {
            NetworkMonitorService.start(context)
        }
        if (BuildConfig.FEATURE_MEDIA && Settings.isMediaButtonsEnabled(context)) {
            MediaButtonService.start(context)
        }
        if (BuildConfig.FEATURE_NOTIFICATION && Settings.isPersistentNotificationEnabled(context)) {
            if (!(BuildConfig.FEATURE_MONITOR &&
                    Settings.isAutoStartEnabled(context) &&
                    Settings.isAnyMonitorRuleEnabled(context))
            ) {
                QuickControlService.start(context)
            }
        }
        if (BuildConfig.FEATURE_SCHEDULE && Settings.isScheduleEnabled(context)) {
            ScheduleAlarmScheduler.scheduleNext(context)
        }
    }
}
