package fuck.wifiadbtoggle.droidvendorssuck

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaButtonReceiver.handleIntent
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

class MediaButtonService : Service() {
    private lateinit var mediaSession: MediaSessionCompat
    private val handler = Handler(Looper.getMainLooper())
    private var pressCount = 0
    private var longPressFired = false
    private val longPressRunnable = Runnable {
        longPressFired = true
        triggerAction(MediaPattern.LONG)
        resetPressState()
    }
    private val finalizeRunnable = Runnable {
        when (pressCount) {
            1 -> triggerAction(MediaPattern.SINGLE)
            2 -> triggerAction(MediaPattern.DOUBLE)
            3 -> triggerAction(MediaPattern.TRIPLE)
        }
        resetPressState()
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "adb_media_buttons").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                    val event = mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    if (event != null) handleKeyEvent(event)
                    return true
                }
            })
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                    .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0f)
                    .build()
            )
            isActive = true
        }
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.isMediaButtonsEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        handleIntent(mediaSession, intent)
        return START_STICKY
    }

    override fun onDestroy() {
        mediaSession.release()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleKeyEvent(event: KeyEvent) {
        if (event.keyCode != KeyEvent.KEYCODE_HEADSETHOOK &&
            event.keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        ) {
            return
        }
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    pressCount++
                    if (Settings.getMediaPattern(this) == MediaPattern.LONG) {
                        handler.postDelayed(longPressRunnable, LONG_PRESS_MS)
                    } else {
                        handler.removeCallbacks(finalizeRunnable)
                        handler.postDelayed(finalizeRunnable, MULTI_PRESS_WINDOW_MS)
                    }
                }
            }
            KeyEvent.ACTION_UP -> {
                if (Settings.getMediaPattern(this) == MediaPattern.LONG && !longPressFired) {
                    handler.removeCallbacks(longPressRunnable)
                    resetPressState()
                }
            }
        }
    }

    private fun triggerAction(triggered: MediaPattern) {
        val selected = Settings.getMediaPattern(this)
        if (selected != triggered) return
        if (!ShellRunner.canUseRoot()) {
            ToastUtils.showShort(this, getString(R.string.toast_action_requires_root))
            return
        }
        AdbWifiController.toggle(this)
    }

    private fun resetPressState() {
        pressCount = 0
        longPressFired = false
        handler.removeCallbacks(longPressRunnable)
        handler.removeCallbacks(finalizeRunnable)
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
            }
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(R.drawable.ic_tile)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIF_CHANNEL_ID = "adb_media_buttons"
        private const val NOTIF_ID = 2001
        private const val MULTI_PRESS_WINDOW_MS = 500L
        private const val LONG_PRESS_MS = 800L

        fun start(context: Context) {
            if (!Settings.isMediaButtonsEnabled(context)) return
            val intent = Intent(context, MediaButtonService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MediaButtonService::class.java)
            context.stopService(intent)
        }
    }
}
