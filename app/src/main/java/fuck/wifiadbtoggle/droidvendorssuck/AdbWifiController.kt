package fuck.wifiadbtoggle.droidvendorssuck

import android.content.Context

object AdbWifiController {
    const val DEFAULT_PORT = 5555

    fun isEnabled(context: Context): Boolean {
        val result = ShellRunner.runPrivileged(context, "getprop service.adb.tcp.port")
        if (!result.success) return false
        val value = result.output.trim()
        if (value.isEmpty()) return false
        return value != "0" && value != "-1"
    }

    fun toggle(context: Context) {
        val enabled = isEnabled(context)
        if (enabled) {
            disable(context)
        } else {
            enable(context)
        }
    }

    fun enable(context: Context) {
        val port = Settings.getAdbPort(context)
        ShellRunner.runPrivileged(context, "setprop service.adb.tcp.port $port; stop adbd; start adbd")
    }

    fun disable(context: Context) {
        ShellRunner.runPrivileged(context, "setprop service.adb.tcp.port -1; stop adbd; start adbd")
    }

    fun getCurrentPort(context: Context): Int? {
        val result = ShellRunner.runPrivileged(context, "getprop service.adb.tcp.port")
        if (!result.success) return null
        val value = result.output.trim()
        val port = value.toIntOrNull() ?: return null
        if (port <= 0) return null
        return port
    }

    fun applyPort(context: Context, port: Int) {
        if (port <= 0) return
        ShellRunner.runPrivileged(context, "setprop service.adb.tcp.port $port; stop adbd; start adbd")
    }
}
