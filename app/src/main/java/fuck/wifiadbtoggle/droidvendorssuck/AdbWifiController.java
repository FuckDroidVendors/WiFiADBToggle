package fuck.wifiadbtoggle.droidvendorssuck;

import android.content.Context;

public final class AdbWifiController {
    private AdbWifiController() {
    }

    public static final int DEFAULT_PORT = 5555;

    public static boolean isEnabled(Context context) {
        ShellRunner.Result result = ShellRunner.runPrivileged(context, "getprop service.adb.tcp.port");
        if (!result.success) return false;
        String value = result.output.trim();
        if (value.isEmpty()) return false;
        return !"0".equals(value) && !"-1".equals(value);
    }

    public static void toggle(Context context) {
        boolean enabled = isEnabled(context);
        if (enabled) {
            disable(context);
        } else {
            enable(context);
        }
    }

    public static void enable(Context context) {
        int port = Settings.getAdbPort(context);
        ShellRunner.runPrivileged(context, "setprop service.adb.tcp.port " + port + "; stop adbd; start adbd");
    }

    public static void enableSilently(Context context) {
        int port = Settings.getAdbPort(context);
        ShellRunner.runPrivilegedSilently("setprop service.adb.tcp.port " + port + "; stop adbd; start adbd");
    }

    public static void disable(Context context) {
        ShellRunner.runPrivileged(context, "setprop service.adb.tcp.port -1; stop adbd; start adbd");
    }

    public static Integer getCurrentPort(Context context) {
        ShellRunner.Result result = ShellRunner.runPrivileged(context, "getprop service.adb.tcp.port");
        if (!result.success) return null;
        String value = result.output.trim();
        Integer port;
        try {
            port = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
        if (port <= 0) return null;
        return port;
    }

    public static void applyPort(Context context, int port) {
        if (port <= 0) return;
        ShellRunner.runPrivileged(context, "setprop service.adb.tcp.port " + port + "; stop adbd; start adbd");
    }
}
