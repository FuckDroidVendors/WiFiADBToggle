package com.example.wifitoggle

import android.content.Context
import android.os.Bundle

class PrivilegedService : IPrivilegedService.Stub {
    @Suppress("unused")
    private val appContext: Context?

    constructor() {
        appContext = null
    }

    @Suppress("unused")
    constructor(context: Context) {
        appContext = context.applicationContext
    }

    override fun runCommand(command: String?): Bundle {
        val bundle = Bundle()
        if (command == null) {
            bundle.putInt("exitCode", 1)
            bundle.putString("output", "null command")
            return bundle
        }

        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val exit = process.waitFor()
            bundle.putInt("exitCode", exit)
            bundle.putString("output", output)
            bundle
        } catch (e: Exception) {
            bundle.putInt("exitCode", 1)
            bundle.putString("output", e.message ?: "shizuku failed")
            bundle
        }
    }
}
