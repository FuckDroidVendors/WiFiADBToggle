package com.example.wifitoggle

import android.content.Context
import android.app.Activity
import android.widget.Toast
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.io.BufferedReader
import java.io.InputStreamReader

object ShellRunner {
    data class Result(val success: Boolean, val output: String)

    fun canUseRoot(): Boolean {
        return runCommandWithSu("id").success
    }

    fun canUseShizuku(): Boolean {
        return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun requestShizukuPermission(activity: Activity) {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(activity, "Shizuku not running", Toast.LENGTH_SHORT).show()
            return
        }
        Shizuku.requestPermission(0)
    }

    fun runPrivileged(context: Context, command: String): Result {
        if (canUseRoot()) {
            return runCommandWithSu(command)
        }
        if (canUseShizuku()) {
            return runCommandWithShizuku(command)
        }
        Toast.makeText(context, "No root or Shizuku permission", Toast.LENGTH_SHORT).show()
        return Result(false, "No root or Shizuku")
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

    private fun runCommandWithShizuku(command: String): Result {
        return try {
            val process: ShizukuRemoteProcess = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
            val exit = process.waitFor()
            Result(exit == 0, output.trim())
        } catch (e: Exception) {
            Result(false, e.message ?: "shizuku failed")
        }
    }
}
