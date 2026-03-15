package fuck.wifiadbtoggle.droidvendorssuck

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface

object NetworkUtils {
    data class IpResult(val address: String, val isIpv6: Boolean)

    fun getActiveIp(context: Context): IpResult? {
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = cm.activeNetwork ?: return null
                val linkProps = cm.getLinkProperties(network) ?: return null
                val v6 = mutableListOf<String>()
                linkProps.linkAddresses.forEach { linkAddr ->
                    val addr = linkAddr.address
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return IpResult(addr.hostAddress, false)
                    }
                    if (addr is Inet6Address && isUsableIpv6(addr)) {
                        v6.add(sanitizeIpv6(addr.hostAddress))
                    }
                }
                if (v6.isNotEmpty()) {
                    return IpResult(v6.first(), true)
                }
            } catch (e: Exception) {
                // Fall through to interface scan
            }
        }
        return getWifiIpv4(context)
            ?: getInterfaceIp()
    }

    fun formatHostForPort(ip: IpResult): String {
        return if (ip.isIpv6) "[${ip.address}]" else ip.address
    }

    private fun getWifiIpv4(context: Context): IpResult? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ip = wifiManager.connectionInfo?.ipAddress ?: 0
            if (ip == 0) return null
            val b1 = ip and 0xFF
            val b2 = (ip shr 8) and 0xFF
            val b3 = (ip shr 16) and 0xFF
            val b4 = (ip shr 24) and 0xFF
            IpResult("$b1.$b2.$b3.$b4", false)
        } catch (_: Exception) {
            null
        }
    }

    private fun getInterfaceIp(): IpResult? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            val v6 = mutableListOf<String>()
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.inetAddresses.toList()) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return IpResult(addr.hostAddress, false)
                    }
                    if (addr is Inet6Address && isUsableIpv6(addr)) {
                        v6.add(sanitizeIpv6(addr.hostAddress))
                    }
                }
            }
            v6.firstOrNull()?.let { IpResult(it, true) }
        } catch (e: Exception) {
            null
        }
    }

    private fun isUsableIpv6(addr: Inet6Address): Boolean {
        return !addr.isLoopbackAddress &&
            !addr.isLinkLocalAddress &&
            !addr.isMulticastAddress &&
            !addr.isAnyLocalAddress
    }

    private fun sanitizeIpv6(address: String): String {
        val percent = address.indexOf('%')
        return if (percent >= 0) address.substring(0, percent) else address
    }
}
