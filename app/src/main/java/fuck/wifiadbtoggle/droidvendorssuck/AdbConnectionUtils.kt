package fuck.wifiadbtoggle.droidvendorssuck

import java.net.Inet6Address
import java.net.InetAddress

object AdbConnectionUtils {

    data class ConnectionInfo(val hosts: List<String>) {
        val count: Int = hosts.size
    }

    fun getActiveConnections(context: android.content.Context, port: Int): ConnectionInfo? {
        val result = ShellRunner.runPrivileged(context, "cat /proc/net/tcp /proc/net/tcp6")
        if (!result.success || result.output.isBlank()) return null
        val hosts = parseProcNet(result.output, port)
        return ConnectionInfo(hosts)
    }

    private fun parseProcNet(output: String, port: Int): List<String> {
        val uniqueHosts = linkedSetOf<String>()
        val portHex = port.toString(16).padStart(4, '0').uppercase()
        output.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("sl")) return@forEach
            val parts = trimmed.split(Regex("\\s+"))
            if (parts.size < 4) return@forEach
            val local = parts[1]
            val remote = parts[2]
            val state = parts[3]
            if (state != "01") return@forEach
            val localPort = local.substringAfter(":", "")
            if (!localPort.equals(portHex, ignoreCase = true)) return@forEach
            val remoteAddrHex = remote.substringBefore(":", "")
            val remotePort = remote.substringAfter(":", "")
            if (remotePort.equals("0000", ignoreCase = true)) return@forEach
            val host = when (remoteAddrHex.length) {
                8 -> ipv4FromHex(remoteAddrHex)
                32 -> ipv6FromHex(remoteAddrHex)
                else -> null
            }
            if (!host.isNullOrBlank()) {
                uniqueHosts.add(host)
            }
        }
        return uniqueHosts.toList()
    }

    private fun ipv4FromHex(hex: String): String? {
        if (hex.length != 8) return null
        return try {
            val bytes = hex.chunked(2).map { it.toInt(16) }
            "${bytes[3]}.${bytes[2]}.${bytes[1]}.${bytes[0]}"
        } catch (_: Exception) {
            null
        }
    }

    private fun ipv6FromHex(hex: String): String? {
        if (hex.length != 32) return null
        return try {
            val bytes = ByteArray(16)
            for (i in 0 until 16) {
                val part = hex.substring(i * 2, i * 2 + 2)
                bytes[i] = part.toInt(16).toByte()
            }
            val addr = InetAddress.getByAddress(bytes)
            if (addr is Inet6Address) {
                if (addr.isLoopbackAddress ||
                    addr.isLinkLocalAddress ||
                    addr.isMulticastAddress ||
                    addr.isAnyLocalAddress
                ) {
                    return null
                }
                addr.hostAddress.substringBefore("%")
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
