package fuck.wifiadbtoggle.droidvendorssuck;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.Build;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public final class NetworkUtils {
    private NetworkUtils() {
    }

    public static final class IpResult {
        public final String address;
        public final boolean isIpv6;

        public IpResult(String address, boolean isIpv6) {
            this.address = address;
            this.isIpv6 = isIpv6;
        }
    }

    public static IpResult getActiveIp(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                Network network = cm.getActiveNetwork();
                if (network == null) return null;
                LinkProperties linkProps = cm.getLinkProperties(network);
                if (linkProps == null) return null;
                List<String> v6 = new ArrayList<>();
                for (LinkAddress linkAddr : linkProps.getLinkAddresses()) {
                    InetAddress addr = linkAddr.getAddress();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return new IpResult(addr.getHostAddress(), false);
                    }
                    if (addr instanceof Inet6Address && isUsableIpv6((Inet6Address) addr)) {
                        v6.add(sanitizeIpv6(addr.getHostAddress()));
                    }
                }
                if (!v6.isEmpty()) {
                    return new IpResult(v6.get(0), true);
                }
            } catch (Exception ignored) {
                // Fall through to interface scan
            }
        }
        IpResult wifi = getWifiIpv4(context);
        return wifi != null ? wifi : getInterfaceIp();
    }

    public static String formatHostForPort(IpResult ip) {
        return ip.isIpv6 ? "[" + ip.address + "]" : ip.address;
    }

    private static IpResult getWifiIpv4(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int ip = wifiManager.getConnectionInfo() != null ? wifiManager.getConnectionInfo().getIpAddress() : 0;
            if (ip == 0) return null;
            int b1 = ip & 0xFF;
            int b2 = (ip >> 8) & 0xFF;
            int b3 = (ip >> 16) & 0xFF;
            int b4 = (ip >> 24) & 0xFF;
            return new IpResult(b1 + "." + b2 + "." + b3 + "." + b4, false);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static IpResult getInterfaceIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) return null;
            List<String> v6 = new ArrayList<>();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return new IpResult(addr.getHostAddress(), false);
                    }
                    if (addr instanceof Inet6Address && isUsableIpv6((Inet6Address) addr)) {
                        v6.add(sanitizeIpv6(addr.getHostAddress()));
                    }
                }
            }
            return v6.isEmpty() ? null : new IpResult(v6.get(0), true);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isUsableIpv6(Inet6Address addr) {
        return !addr.isLoopbackAddress()
            && !addr.isLinkLocalAddress()
            && !addr.isMulticastAddress()
            && !addr.isAnyLocalAddress();
    }

    private static String sanitizeIpv6(String address) {
        int percent = address.indexOf('%');
        return percent >= 0 ? address.substring(0, percent) : address;
    }
}
