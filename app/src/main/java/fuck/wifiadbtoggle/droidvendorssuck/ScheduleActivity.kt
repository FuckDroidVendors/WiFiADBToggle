package fuck.wifiadbtoggle.droidvendorssuck

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat
import java.util.Calendar

class ScheduleActivity : AppCompatActivity() {
    private lateinit var calendarView: CalendarView
    private lateinit var list: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var addButton: Button
    private lateinit var adapter: ScheduleAdapter
    private lateinit var db: ScheduleDbHelper
    private var selectedDayMillis: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        db = ScheduleDbHelper(this)
        calendarView = findViewById(R.id.scheduleCalendar)
        list = findViewById(R.id.scheduleList)
        emptyView = findViewById(R.id.scheduleEmpty)
        addButton = findViewById(R.id.scheduleAdd)

        adapter = ScheduleAdapter { entry -> showEditDialog(entry) }
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            selectedDayMillis = cal.timeInMillis
            loadForSelectedDay()
        }

        addButton.setOnClickListener {
            showEditDialog(null)
        }

        selectedDayMillis = calendarView.date
        loadForSelectedDay()
    }

    private fun loadForSelectedDay() {
        val (start, end) = ScheduleManager.getDayBounds(selectedDayMillis)
        val items = db.listForDay(start, end)
        adapter.submit(items)
        emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showEditDialog(existing: ScheduleEntry?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_schedule_edit, null)
        val titleInput = view.findViewById<EditText>(R.id.scheduleTitleInput)
        val startButton = view.findViewById<Button>(R.id.scheduleStartButton)
        val endButton = view.findViewById<Button>(R.id.scheduleEndButton)
        val modeGroup = view.findViewById<RadioGroup>(R.id.scheduleModeGroup)
        val modeForceOn = view.findViewById<RadioButton>(R.id.scheduleModeForceOn)
        val modeForceOff = view.findViewById<RadioButton>(R.id.scheduleModeForceOff)
        val modeRespect = view.findViewById<RadioButton>(R.id.scheduleModeRespect)
        val enabledSwitch = view.findViewById<Switch>(R.id.scheduleEnabled)

        val calStart = Calendar.getInstance()
        val calEnd = Calendar.getInstance()
        if (existing != null) {
            titleInput.setText(existing.title)
            calStart.timeInMillis = existing.startMillis
            calEnd.timeInMillis = existing.endMillis
            when (existing.mode) {
                ScheduleMode.FORCE_ON -> modeForceOn.isChecked = true
                ScheduleMode.FORCE_OFF -> modeForceOff.isChecked = true
                ScheduleMode.RESPECT -> modeRespect.isChecked = true
            }
            enabledSwitch.isChecked = existing.enabled
        } else {
            val day = Calendar.getInstance()
            day.timeInMillis = selectedDayMillis
            calStart.set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH), 8, 0, 0)
            calStart.set(Calendar.MILLISECOND, 0)
            calEnd.timeInMillis = calStart.timeInMillis + 2 * 60 * 60 * 1000L
            modeRespect.isChecked = true
            enabledSwitch.isChecked = true
        }

        val fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        fun updateButtons() {
            startButton.text = fmt.format(calStart.timeInMillis)
            endButton.text = fmt.format(calEnd.timeInMillis)
        }
        updateButtons()

        startButton.setOnClickListener {
            showDateTimePicker(calStart) { updateButtons() }
        }
        endButton.setOnClickListener {
            showDateTimePicker(calEnd) { updateButtons() }
        }

        val builder = AlertDialog.Builder(this)
            .setTitle(if (existing == null) getString(R.string.schedule_add) else getString(R.string.schedule_edit))
            .setView(view)
            .setPositiveButton(getString(R.string.schedule_save), null)
            .setNegativeButton(getString(R.string.schedule_cancel), null)
        if (existing != null) {
            builder.setNeutralButton(getString(R.string.schedule_delete), null)
        }
        val dialog = builder.create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (calEnd.timeInMillis <= calStart.timeInMillis) {
                    ToastUtils.showShort(this, getString(R.string.schedule_invalid_time))
                    return@setOnClickListener
                }
                val mode = when (modeGroup.checkedRadioButtonId) {
                    R.id.scheduleModeForceOn -> ScheduleMode.FORCE_ON
                    R.id.scheduleModeForceOff -> ScheduleMode.FORCE_OFF
                    else -> ScheduleMode.RESPECT
                }
                val entry = ScheduleEntry(
                    id = existing?.id ?: 0L,
                    title = titleInput.text.toString().ifBlank { "Schedule" },
                    startMillis = calStart.timeInMillis,
                    endMillis = calEnd.timeInMillis,
                    mode = mode,
                    enabled = enabledSwitch.isChecked
                )
                if (existing == null) {
                    db.insert(entry)
                } else {
                    db.update(entry)
                }
                ScheduleManager.applyScheduleNow(this)
                ScheduleAlarmScheduler.scheduleNext(this)
                loadForSelectedDay()
                dialog.dismiss()
            }

            if (existing != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    db.delete(existing.id)
                    ScheduleManager.applyScheduleNow(this)
                    ScheduleAlarmScheduler.scheduleNext(this)
                    loadForSelectedDay()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun showDateTimePicker(target: Calendar, onDone: () -> Unit) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = target.timeInMillis
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        target.set(year, month, dayOfMonth, hour, minute, 0)
                        target.set(Calendar.MILLISECOND, 0)
                        onDone()
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
