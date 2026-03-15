package fuck.wifiadbtoggle.droidvendorssuck

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {
    fun getActiveIpv4(context: Context): String? {
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = cm.activeNetwork ?: return null
                val linkProps = cm.getLinkProperties(network) ?: return null
                linkProps.linkAddresses.forEach { linkAddr ->
                    val addr = linkAddr.address
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            } catch (e: Exception) {
                // Fall through to interface scan
            }
        }
        return getInterfaceIpv4()
    }

    private fun getInterfaceIpv4(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.inetAddresses.toList()) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
