package fuck.wifiadbtoggle.droidvendorssuck;

import android.content.Context;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AdbConnectionUtils {
    private AdbConnectionUtils() {
    }

    public static final class ConnectionInfo {
        public final List<String> hosts;
        public final int count;

        public ConnectionInfo(List<String> hosts) {
            this.hosts = hosts;
            this.count = hosts.size();
        }
    }

    public static ConnectionInfo getActiveConnections(Context context, int port) {
        ShellRunner.Result result = ShellRunner.runPrivileged(context, "cat /proc/net/tcp /proc/net/tcp6");
        if (!result.success || result.output.trim().isEmpty()) return null;
        List<String> hosts = parseProcNet(result.output, port);
        return new ConnectionInfo(hosts);
    }

    private static List<String> parseProcNet(String output, int port) {
        Set<String> uniqueHosts = new LinkedHashSet<>();
        String portHex = String.format(Locale.US, "%04X", port);
        String[] lines = output.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("sl")) continue;
            String[] parts = trimmed.split("\\s+");
            if (parts.length < 4) continue;
            String local = parts[1];
            String remote = parts[2];
            String state = parts[3];
            if (!"01".equals(state)) continue;
            String[] localParts = local.split(":", 2);
            String localPort = localParts.length > 1 ? localParts[1] : "";
            if (!localPort.equalsIgnoreCase(portHex)) continue;
            String[] remoteParts = remote.split(":", 2);
            String remoteAddrHex = remoteParts.length > 0 ? remoteParts[0] : "";
            String remotePort = remoteParts.length > 1 ? remoteParts[1] : "";
            if ("0000".equalsIgnoreCase(remotePort)) continue;
            String host;
            if (remoteAddrHex.length() == 8) {
                host = ipv4FromHex(remoteAddrHex);
            } else if (remoteAddrHex.length() == 32) {
                host = ipv6FromHex(remoteAddrHex);
            } else {
                host = null;
            }
            if (host != null && !host.trim().isEmpty()) {
                uniqueHosts.add(host);
            }
        }
        return new ArrayList<>(uniqueHosts);
    }

    private static String ipv4FromHex(String hex) {
        if (hex.length() != 8) return null;
        try {
            int[] bytes = new int[4];
            for (int i = 0; i < 4; i++) {
                String part = hex.substring(i * 2, i * 2 + 2);
                bytes[i] = Integer.parseInt(part, 16);
            }
            return String.format(Locale.US, "%d.%d.%d.%d", bytes[3], bytes[2], bytes[1], bytes[0]);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String ipv6FromHex(String hex) {
        if (hex.length() != 32) return null;
        try {
            byte[] bytes = new byte[16];
            for (int i = 0; i < 16; i++) {
                String part = hex.substring(i * 2, i * 2 + 2);
                bytes[i] = (byte) Integer.parseInt(part, 16);
            }
            InetAddress addr = InetAddress.getByAddress(bytes);
            if (addr instanceof Inet6Address) {
                Inet6Address inet6 = (Inet6Address) addr;
                if (inet6.isLoopbackAddress() ||
                    inet6.isLinkLocalAddress() ||
                    inet6.isMulticastAddress() ||
                    inet6.isAnyLocalAddress()
                ) {
                    return null;
                }
                String host = inet6.getHostAddress();
                int percent = host.indexOf('%');
                return percent >= 0 ? host.substring(0, percent) : host;
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
