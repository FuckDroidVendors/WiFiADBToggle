package fuck.wifiadbtoggle.droidvendorssuck

import android.content.Context
import android.widget.Toast
import java.io.BufferedReader

object ShellRunner {
    data class Result(val success: Boolean, val output: String)

    fun canUseRoot(): Boolean {
        return runCommandWithSu("id").success
    }

    fun requestRoot(context: Context): Result {
        val result = runCommandWithSu("id")
        if (!result.success) {
            Toast.makeText(context, context.getString(R.string.toast_no_root_permission), Toast.LENGTH_SHORT).show()
        }
        return result
    }

    fun runPrivileged(context: Context, command: String): Result {
        if (canUseRoot()) {
            return runCommandWithSu(command)
        }
        Toast.makeText(context, context.getString(R.string.toast_no_root_permission), Toast.LENGTH_SHORT).show()
        return Result(false, context.getString(R.string.result_no_root_permission))
    }

    private fun runCommandWithSu(command: String): Result {
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
            val exit = process.waitFor()
            Result(exit == 0, output.trim())
        } catch (e: Exception) {
            Result(false, e.message ?: "su failed")
        }
    }
}
