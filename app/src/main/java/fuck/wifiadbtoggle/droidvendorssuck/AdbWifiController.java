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
        if (BuildConfig.SYSTEM_APP_BUILD && applyPortWithSystemProperties(port)) {
            return;
        }
        ShellRunner.runPrivileged(context, "setprop service.adb.tcp.port " + port + "; stop adbd; start adbd");
    }

    public static void enableSilently(Context context) {
        int port = Settings.getAdbPort(context);
        if (BuildConfig.SYSTEM_APP_BUILD && applyPortWithSystemProperties(port)) {
            return;
        }
        ShellRunner.runPrivilegedSilently("setprop service.adb.tcp.port " + port + "; stop adbd; start adbd");
    }

    public static void enableSilentlyWithPort(int port) {
        if (port <= 0) port = DEFAULT_PORT;
        if (BuildConfig.SYSTEM_APP_BUILD && applyPortWithSystemProperties(port)) {
            return;
        }
        ShellRunner.runPrivilegedSilently("setprop service.adb.tcp.port " + port + "; stop adbd; start adbd");
    }

    public static void disable(Context context) {
        if (BuildConfig.SYSTEM_APP_BUILD && applyPortWithSystemProperties(-1)) {
            return;
        }
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
        if (BuildConfig.SYSTEM_APP_BUILD && applyPortWithSystemProperties(port)) {
            return;
        }
        ShellRunner.runPrivileged(context, "setprop service.adb.tcp.port " + port + "; stop adbd; start adbd");
    }

    private static boolean applyPortWithSystemProperties(int port) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method set =
                systemProperties.getMethod("set", String.class, String.class);
            set.invoke(null, "service.adb.tcp.port", String.valueOf(port));
            set.invoke(null, "ctl.stop", "adbd");
            set.invoke(null, "ctl.start", "adbd");
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
