package fuck.wifiadbtoggle.droidvendorssuck

import android.content.Context
import java.util.Calendar

object ScheduleManager {
    fun getActiveMode(context: Context, now: Long = System.currentTimeMillis()): ScheduleMode? {
        val entries = ScheduleDbHelper(context).listAll().filter { it.enabled && it.startMillis <= now && it.endMillis > now }
        if (entries.isEmpty()) return null
        // Priority: FORCE_OFF > FORCE_ON > RESPECT
        return when {
            entries.any { it.mode == ScheduleMode.FORCE_OFF } -> ScheduleMode.FORCE_OFF
            entries.any { it.mode == ScheduleMode.FORCE_ON } -> ScheduleMode.FORCE_ON
            else -> ScheduleMode.RESPECT
        }
    }

    fun applyScheduleNow(context: Context) {
        val mode = getActiveMode(context) ?: return
        if (!ShellRunner.canUseRoot()) return
        when (mode) {
            ScheduleMode.FORCE_ON -> AdbWifiController.enable(context)
            ScheduleMode.FORCE_OFF -> AdbWifiController.disable(context)
            ScheduleMode.RESPECT -> {
                // Respect network rules; no direct action.
            }
        }
    }

    fun getNextBoundary(context: Context, now: Long = System.currentTimeMillis()): Long? {
        val entries = ScheduleDbHelper(context).listAll().filter { it.enabled }
        var next: Long? = null
        for (entry in entries) {
            if (entry.startMillis > now) next = minMillis(next, entry.startMillis)
            if (entry.endMillis > now) next = minMillis(next, entry.endMillis)
        }
        return next
    }

    fun getDayBounds(dayMillis: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dayMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val end = cal.timeInMillis
        return start to end
    }

    private fun minMillis(a: Long?, b: Long): Long {
        return if (a == null || b < a) b else a
    }
}
