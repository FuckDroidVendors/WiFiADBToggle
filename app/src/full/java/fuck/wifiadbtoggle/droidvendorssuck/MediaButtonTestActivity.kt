package fuck.wifiadbtoggle.droidvendorssuck

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MediaButtonTestActivity : Activity() {
    private lateinit var eventText: TextView
    private lateinit var countText: TextView
    private var count = 0
    private var receiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val name = intent?.getStringExtra(MediaButtonService.EXTRA_EVENT_NAME) ?: "unknown"
            val action = intent?.getStringExtra(MediaButtonService.EXTRA_EVENT_ACTION) ?: "?"
            count++
            eventText.text = getString(R.string.media_test_event, "$name $action")
            countText.text = getString(R.string.media_test_count, count)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_button_test)
        eventText = findViewById(R.id.mediaTestEvent)
        countText = findViewById(R.id.mediaTestCount)
        findViewById<Button>(R.id.mediaTestClose).setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(MediaButtonService.ACTION_MEDIA_BUTTON_EVENT)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }
        receiverRegistered = true
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(receiver)
            receiverRegistered = false
        }
        super.onStop()
    }
}
