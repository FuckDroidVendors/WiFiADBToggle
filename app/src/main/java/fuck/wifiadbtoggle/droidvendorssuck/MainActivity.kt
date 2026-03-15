package fuck.wifiadbtoggle.droidvendorssuck

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var autoStartSwitch: Switch
    private lateinit var keepAwakeSwitch: Switch
    private lateinit var keepScreenOnSwitch: Switch
    private lateinit var autoEnableEthernetSwitch: Switch
    private lateinit var disableOnDisconnectSwitch: Switch
    private lateinit var autoEnableSsidSwitch: Switch
    private lateinit var filterBssidSwitch: Switch
    private lateinit var ssidListEdit: EditText
    private lateinit var bssidListEdit: EditText
    private lateinit var addCurrentWifiButton: Button
    private lateinit var mediaButtonsSwitch: Switch
    private lateinit var persistentNotificationSwitch: Switch
    private lateinit var mediaPatternGroup: RadioGroup
    private lateinit var mediaPatternSingle: RadioButton
    private lateinit var mediaPatternDouble: RadioButton
    private lateinit var mediaPatternTriple: RadioButton
    private lateinit var mediaPatternLong: RadioButton
    private lateinit var scheduleEnabledSwitch: Switch
    private lateinit var openScheduleButton: Button
    private val monitorHandler = Handler(Looper.getMainLooper())
    private val monitorRestartRunnable = Runnable { NetworkMonitorService.start(this) }

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                addCurrentWifi()
            } else {
                ToastUtils.showShort(this, getString(R.string.toast_location_permission_required))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val toggle = findViewById<Button>(R.id.testToggle)
        autoStartSwitch = findViewById(R.id.settingAutoStart)
        keepAwakeSwitch = findViewById(R.id.settingKeepAwake)
        keepScreenOnSwitch = findViewById(R.id.settingKeepScreenOn)
        autoEnableEthernetSwitch = findViewById(R.id.settingAutoEnableEthernet)
        disableOnDisconnectSwitch = findViewById(R.id.settingDisableOnDisconnect)
        autoEnableSsidSwitch = findViewById(R.id.settingAutoEnableSsid)
        filterBssidSwitch = findViewById(R.id.settingFilterBssid)
        ssidListEdit = findViewById(R.id.settingSsidList)
        bssidListEdit = findViewById(R.id.settingBssidList)
        addCurrentWifiButton = findViewById(R.id.addCurrentWifi)
        mediaButtonsSwitch = findViewById(R.id.settingMediaButtons)
        persistentNotificationSwitch = findViewById(R.id.settingPersistentNotification)
        mediaPatternGroup = findViewById(R.id.settingMediaPatternGroup)
        mediaPatternSingle = findViewById(R.id.mediaPatternSingle)
        mediaPatternDouble = findViewById(R.id.mediaPatternDouble)
        mediaPatternTriple = findViewById(R.id.mediaPatternTriple)
        mediaPatternLong = findViewById(R.id.mediaPatternLong)
        scheduleEnabledSwitch = findViewById(R.id.settingScheduleEnabled)
        openScheduleButton = findViewById(R.id.openSchedule)

        toggle.setOnClickListener {
            if (!ShellRunner.canUseRoot()) {
                ShellRunner.requestRoot(this)
            }
            if (ShellRunner.canUseRoot()) {
                AdbWifiController.toggle(this)
                ScheduleManager.applyScheduleNow(this)
                updateStatus()
            }
        }

        bindSettings()
        updateStatus()
        ScheduleAlarmScheduler.scheduleNext(this)
    }

    override fun onDestroy() {
        monitorHandler.removeCallbacks(monitorRestartRunnable)
        super.onDestroy()
    }

    private fun updateStatus() {
        val root = ShellRunner.canUseRoot()
        val enabled = AdbWifiController.isEnabled(this)
        val ip = NetworkUtils.getActiveIpv4(this)
        val yes = getString(R.string.value_yes)
        val no = getString(R.string.value_no)
        val on = getString(R.string.value_on)
        val off = getString(R.string.value_off)
        val lines = mutableListOf(
            getString(R.string.status_root, if (root) yes else no),
            getString(R.string.status_wifi_adb, if (enabled) on else off)
        )
        val ipText = if (ip != null) getString(R.string.ip_with_port, ip) else getString(R.string.value_no_ip)
        lines.add(getString(R.string.status_ip, ipText))
        statusText.text = lines.joinToString("\n")
    }

    private fun bindSettings() {
        autoStartSwitch.isChecked = Settings.isAutoStartEnabled(this)
        keepAwakeSwitch.isChecked = Settings.isKeepAwakeEnabled(this)
        keepScreenOnSwitch.isChecked = Settings.isKeepScreenOnEnabled(this)
        autoEnableEthernetSwitch.isChecked = Settings.isAutoEnableEthernetEnabled(this)
        disableOnDisconnectSwitch.isChecked = Settings.isDisableOnDisconnectEnabled(this)
        autoEnableSsidSwitch.isChecked = Settings.isAutoEnableSsidEnabled(this)
        filterBssidSwitch.isChecked = Settings.isFilterBssidEnabled(this)
        ssidListEdit.setText(Settings.getSsidList(this))
        bssidListEdit.setText(Settings.getBssidList(this))
        applyKeepScreenOn(Settings.isKeepScreenOnEnabled(this))
        mediaButtonsSwitch.isChecked = Settings.isMediaButtonsEnabled(this)
        persistentNotificationSwitch.isChecked = Settings.isPersistentNotificationEnabled(this)
        scheduleEnabledSwitch.isChecked = Settings.isScheduleEnabled(this)
        updateMediaPatternVisibility(mediaButtonsSwitch.isChecked)

        when (Settings.getMediaPattern(this)) {
            MediaPattern.SINGLE -> mediaPatternSingle.isChecked = true
            MediaPattern.DOUBLE -> mediaPatternDouble.isChecked = true
            MediaPattern.TRIPLE -> mediaPatternTriple.isChecked = true
            MediaPattern.LONG -> mediaPatternLong.isChecked = true
        }

        autoStartSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.setAutoStartEnabled(this, isChecked)
            if (isChecked) {
                NetworkMonitorService.start(this)
                ToastUtils.showShort(this, getString(R.string.toast_monitor_started))
            } else {
                NetworkMonitorService.stop(this)
                ToastUtils.showShort(this, getString(R.string.toast_monitor_stopped))
            }
        }

        keepAwakeSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.setKeepAwakeEnabled(this, isChecked)
            scheduleMonitorRestart()
        }

        keepScreenOnSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.setKeepScreenOnEnabled(this, isChecked)
            applyKeepScreenOn(isChecked)
        }

        autoEnableEthernetSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.setAutoEnableEthernetEnabled(this, isChecked)
            scheduleMonitorRestart()
        }

        disableOnDisconnectSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.setDisableOnDisconnectEnabled(this, isChecked)
            scheduleMonitorRestart()
        }

        autoEnableSsidSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.setAutoEnableSsidEnabled(this, isChecked)
            scheduleMonitorRestart()
        }

        filterBssidSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.setFilterBssidEnabled(this, isChecked)
            scheduleMonitorRestart()
        }

        ssidListEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                Settings.setSsidList(this@MainActivity, s?.toString() ?: "")
                scheduleMonitorRestart()
            }
        })

        bssidListEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                Settings.setBssidList(this@MainActivity, s?.toString() ?: "")
                scheduleMonitorRestart()
            }
        })

        addCurrentWifiButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                addCurrentWifi()
            }
        }

        mediaButtonsSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.setMediaButtonsEnabled(this, isChecked)
            updateMediaPatternVisibility(isChecked)
            if (isChecked) {
                MediaButtonService.start(this)
                ToastUtils.showShort(this, getString(R.string.toast_media_listener_started))
            } else {
                MediaButtonService.stop(this)
                ToastUtils.showShort(this, getString(R.string.toast_media_listener_stopped))
            }
        }

        persistentNotificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.setPersistentNotificationEnabled(this, isChecked)
            if (isChecked) {
                QuickControlService.start(this)
            } else {
                QuickControlService.stop(this)
            }
        }

        scheduleEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.setScheduleEnabled(this, isChecked)
            if (isChecked) {
                ScheduleManager.applyScheduleNow(this)
            }
            ScheduleAlarmScheduler.scheduleNext(this)
        }

        mediaPatternGroup.setOnCheckedChangeListener { _, checkedId ->
            val pattern = when (checkedId) {
                R.id.mediaPatternSingle -> MediaPattern.SINGLE
                R.id.mediaPatternDouble -> MediaPattern.DOUBLE
                R.id.mediaPatternTriple -> MediaPattern.TRIPLE
                R.id.mediaPatternLong -> MediaPattern.LONG
                else -> MediaPattern.DOUBLE
            }
            Settings.setMediaPattern(this, pattern)
        }

        openScheduleButton.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }

    }

    private fun scheduleMonitorRestart() {
        monitorHandler.removeCallbacks(monitorRestartRunnable)
        if (!Settings.isAutoStartEnabled(this) || !Settings.isAnyMonitorRuleEnabled(this)) {
            NetworkMonitorService.stop(this)
            return
        }
        monitorHandler.postDelayed(monitorRestartRunnable, 400L)
    }

    private fun updateMediaPatternVisibility(enabled: Boolean) {
        val visibility = if (enabled) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.settingMediaPatternLabel).visibility = visibility
        mediaPatternGroup.visibility = visibility
    }

    private fun addCurrentWifi() {
        val info = getCurrentWifiInfo()
        if (info == null) {
            ToastUtils.showShort(this, getString(R.string.toast_wifi_info_unavailable))
            return
        }
        Settings.addSsid(this, info.first)
        ssidListEdit.setText(Settings.getSsidList(this))
        info.second?.let {
            Settings.addBssid(this, it)
            bssidListEdit.setText(Settings.getBssidList(this))
        }
        NetworkMonitorService.start(this)
    }

    private fun getCurrentWifiInfo(): Pair<String, String?>? {
        return try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo ?: return null
            val ssid = info.ssid?.takeIf { it != "<unknown ssid>" } ?: return null
            ssid to info.bssid
        } catch (_: Exception) {
            null
        }
    }

    private fun applyKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
