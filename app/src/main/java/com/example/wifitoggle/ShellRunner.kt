package com.example.wifitoggle

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
            return runCommandWithShizuku(context, command)
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

    private var shizukuService: IPrivilegedService? = null
    @Volatile
    private var bindLatch: CountDownLatch? = null
    @Volatile
    private var binderDeadListenerAdded = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            shizukuService = IPrivilegedService.Stub.asInterface(service)
            bindLatch?.countDown()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shizukuService = null
        }
    }

    private fun ensureShizukuService(context: Context): IPrivilegedService? {
        if (!canUseShizuku()) return null
        shizukuService?.let { return it }

        val latch = CountDownLatch(1)
        bindLatch = latch
        val args = Shizuku.UserServiceArgs(
            ComponentName(context, PrivilegedService::class.java)
        )
            .daemon(false)
            .tag("privileged_shell")
            .version(1)

        Shizuku.bindUserService(args, serviceConnection)

        if (!binderDeadListenerAdded) {
            Shizuku.addBinderDeadListener {
                shizukuService = null
            }
            binderDeadListenerAdded = true
        }

        // Wait briefly for the service connection
        latch.await(500, TimeUnit.MILLISECONDS)
        bindLatch = null
        return shizukuService
    }

    private fun runCommandWithShizuku(context: Context, command: String): Result {
        return try {
            val service = ensureShizukuService(context)
                ?: return Result(false, "Shizuku service not available")
            val bundle: Bundle = service.runCommand(command)
            val exit = bundle.getInt("exitCode", 1)
            val output = bundle.getString("output", "") ?: ""
            Result(exit == 0, output.trim())
        } catch (e: Exception) {
            Result(false, e.message ?: "shizuku failed")
        }
    }
}
