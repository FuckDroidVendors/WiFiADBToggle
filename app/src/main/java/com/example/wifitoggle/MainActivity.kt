package com.example.wifitoggle

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { _, _ -> updateStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val request = findViewById<Button>(R.id.requestShizuku)
        val toggle = findViewById<Button>(R.id.testToggle)

        request.setOnClickListener {
            ShellRunner.requestShizukuPermission(this)
            updateStatus()
        }

        toggle.setOnClickListener {
            AdbWifiController.toggle(this)
            updateStatus()
        }

        updateStatus()
    }

    override fun onStart() {
        super.onStart()
        Shizuku.addRequestPermissionResultListener(permissionListener)
    }

    override fun onStop() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        super.onStop()
    }

    private fun updateStatus() {
        val root = ShellRunner.canUseRoot()
        val shizuku = ShellRunner.canUseShizuku()
        val enabled = AdbWifiController.isEnabled(this)
        val ip = NetworkUtils.getLocalIpV4()
        statusText.text = buildString {
            append("Root: ")
            append(if (root) "yes" else "no")
            append("\nShizuku: ")
            append(if (shizuku) "yes" else "no")
            append("\nWiFi ADB: ")
            append(if (enabled) "on" else "off")
            if (ip != null) {
                append("\nIP: ")
                append(ip)
                append(":5555")
            }
        }
    }
}
