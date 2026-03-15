package fuck.wifiadbtoggle.droidvendorssuck

data class ScheduleEntry(
    val id: Long = 0L,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val mode: ScheduleMode,
    val enabled: Boolean = true
)

enum class ScheduleMode(val id: String) {
    FORCE_ON("force_on"),
    FORCE_OFF("force_off"),
    RESPECT("respect");

    companion object {
        fun fromId(id: String): ScheduleMode {
            return values().firstOrNull { it.id == id } ?: RESPECT
        }
    }
}
