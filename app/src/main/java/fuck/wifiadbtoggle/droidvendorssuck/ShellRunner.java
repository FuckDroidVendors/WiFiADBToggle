package fuck.wifiadbtoggle.droidvendorssuck;

import android.content.Context;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class ShellRunner {
    private ShellRunner() {
    }

    public static final class Result {
        public final boolean success;
        public final String output;

        public Result(boolean success, String output) {
            this.success = success;
            this.output = output;
        }
    }

    public static boolean canUseRoot() {
        return runCommandWithSu("id").success;
    }

    public static Result requestRoot(Context context) {
        Result result = runCommandWithSu("id");
        if (!result.success) {
            Toast.makeText(context, context.getString(R.string.toast_no_root_permission), Toast.LENGTH_SHORT).show();
        }
        return result;
    }

    public static Result runPrivileged(Context context, String command) {
        if (canUseRoot()) {
            return runCommandWithSu(command);
        }
        Toast.makeText(context, context.getString(R.string.toast_no_root_permission), Toast.LENGTH_SHORT).show();
        return new Result(false, context.getString(R.string.result_no_root_permission));
    }

    public static Result runPrivilegedSilently(String command) {
        return runCommandWithSu(command);
    }

    private static Result runCommandWithSu(String command) {
        try {
            Process process = new ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(line);
                }
                output = sb.toString();
            }
            int exit = process.waitFor();
            return new Result(exit == 0, output.trim());
        } catch (Exception e) {
            return new Result(false, e.getMessage() != null ? e.getMessage() : "su failed");
        }
    }
}
