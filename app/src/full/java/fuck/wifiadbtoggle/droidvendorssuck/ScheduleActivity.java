package fuck.wifiadbtoggle.droidvendorssuck;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.DateFormat;
import java.util.Calendar;

public class ScheduleActivity extends Activity {
    private CalendarView calendarView;
    private RecyclerView list;
    private TextView emptyView;
    private Button addButton;
    private ScheduleAdapter adapter;
    private ScheduleDbHelper db;
    private long selectedDayMillis = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        db = new ScheduleDbHelper(this);
        calendarView = findViewById(R.id.scheduleCalendar);
        list = findViewById(R.id.scheduleList);
        emptyView = findViewById(R.id.scheduleEmpty);
        addButton = findViewById(R.id.scheduleAdd);

        adapter = new ScheduleAdapter(new ScheduleAdapter.OnClickListener() {
            @Override
            public void onClick(ScheduleEntry entry) {
                showEditDialog(entry);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, dayOfMonth, 0, 0, 0);
                cal.set(Calendar.MILLISECOND, 0);
                selectedDayMillis = cal.getTimeInMillis();
                loadForSelectedDay();
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditDialog(null);
            }
        });

        selectedDayMillis = calendarView.getDate();
        loadForSelectedDay();
    }

    private void loadForSelectedDay() {
        ScheduleManager.DayBounds bounds = ScheduleManager.getDayBounds(selectedDayMillis);
        java.util.List<ScheduleEntry> items = db.listForDay(bounds.startMillis, bounds.endMillis);
        adapter.submit(items);
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showEditDialog(final ScheduleEntry existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_schedule_edit, null);
        final EditText titleInput = view.findViewById(R.id.scheduleTitleInput);
        final Button startButton = view.findViewById(R.id.scheduleStartButton);
        final Button endButton = view.findViewById(R.id.scheduleEndButton);
        final RadioGroup modeGroup = view.findViewById(R.id.scheduleModeGroup);
        final RadioButton modeForceOn = view.findViewById(R.id.scheduleModeForceOn);
        final RadioButton modeForceOff = view.findViewById(R.id.scheduleModeForceOff);
        final RadioButton modeRespect = view.findViewById(R.id.scheduleModeRespect);
        final Switch enabledSwitch = view.findViewById(R.id.scheduleEnabled);

        final Calendar calStart = Calendar.getInstance();
        final Calendar calEnd = Calendar.getInstance();
        if (existing != null) {
            titleInput.setText(existing.title);
            calStart.setTimeInMillis(existing.startMillis);
            calEnd.setTimeInMillis(existing.endMillis);
            switch (existing.mode) {
                case FORCE_ON:
                    modeForceOn.setChecked(true);
                    break;
                case FORCE_OFF:
                    modeForceOff.setChecked(true);
                    break;
                case RESPECT:
                default:
                    modeRespect.setChecked(true);
                    break;
            }
            enabledSwitch.setChecked(existing.enabled);
        } else {
            Calendar day = Calendar.getInstance();
            day.setTimeInMillis(selectedDayMillis);
            calStart.set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH), 8, 0, 0);
            calStart.set(Calendar.MILLISECOND, 0);
            calEnd.setTimeInMillis(calStart.getTimeInMillis() + 2 * 60 * 60 * 1000L);
            modeRespect.setChecked(true);
            enabledSwitch.setChecked(true);
        }

        final DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        updateButtons(startButton, endButton, calStart, calEnd, fmt);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateTimePicker(calStart, new Runnable() {
                    @Override
                    public void run() {
                        updateButtons(startButton, endButton, calStart, calEnd, fmt);
                    }
                });
            }
        });
        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateTimePicker(calEnd, new Runnable() {
                    @Override
                    public void run() {
                        updateButtons(startButton, endButton, calStart, calEnd, fmt);
                    }
                });
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(existing == null ? getString(R.string.schedule_add) : getString(R.string.schedule_edit))
            .setView(view)
            .setPositiveButton(getString(R.string.schedule_save), null)
            .setNegativeButton(getString(R.string.schedule_cancel), null);
        if (existing != null) {
            builder.setNeutralButton(getString(R.string.schedule_delete), null);
        }
        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterfaceOnShowListener(dialog, existing, titleInput, calStart, calEnd,
            modeGroup, enabledSwitch));

        dialog.show();
    }

    private static final class DialogInterfaceOnShowListener implements android.content.DialogInterface.OnShowListener {
        private final AlertDialog dialog;
        private final ScheduleEntry existing;
        private final EditText titleInput;
        private final Calendar calStart;
        private final Calendar calEnd;
        private final RadioGroup modeGroup;
        private final Switch enabledSwitch;

        DialogInterfaceOnShowListener(
            AlertDialog dialog,
            ScheduleEntry existing,
            EditText titleInput,
            Calendar calStart,
            Calendar calEnd,
            RadioGroup modeGroup,
            Switch enabledSwitch
        ) {
            this.dialog = dialog;
            this.existing = existing;
            this.titleInput = titleInput;
            this.calStart = calStart;
            this.calEnd = calEnd;
            this.modeGroup = modeGroup;
            this.enabledSwitch = enabledSwitch;
        }

        @Override
        public void onShow(android.content.DialogInterface dialogInterface) {
            final ScheduleActivity activity = (ScheduleActivity) dialog.getContext();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (calEnd.getTimeInMillis() <= calStart.getTimeInMillis()) {
                        ToastUtils.showShort(activity, activity.getString(R.string.schedule_invalid_time));
                        return;
                    }
                    ScheduleMode mode;
                    int checkedId = modeGroup.getCheckedRadioButtonId();
                    if (checkedId == R.id.scheduleModeForceOn) {
                        mode = ScheduleMode.FORCE_ON;
                    } else if (checkedId == R.id.scheduleModeForceOff) {
                        mode = ScheduleMode.FORCE_OFF;
                    } else {
                        mode = ScheduleMode.RESPECT;
                    }
                    long id = existing == null ? 0L : existing.id;
                    String title = titleInput.getText().toString();
                    if (title.trim().isEmpty()) {
                        title = "Schedule";
                    }
                    ScheduleEntry entry = new ScheduleEntry(
                        id,
                        title,
                        calStart.getTimeInMillis(),
                        calEnd.getTimeInMillis(),
                        mode,
                        enabledSwitch.isChecked()
                    );
                    if (existing == null) {
                        activity.db.insert(entry);
                    } else {
                        activity.db.update(entry);
                    }
                    ScheduleManager.applyScheduleNow(activity);
                    ScheduleAlarmScheduler.scheduleNext(activity);
                    activity.loadForSelectedDay();
                    dialog.dismiss();
                }
            });

            if (existing != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.db.delete(existing.id);
                        ScheduleManager.applyScheduleNow(activity);
                        ScheduleAlarmScheduler.scheduleNext(activity);
                        activity.loadForSelectedDay();
                        dialog.dismiss();
                    }
                });
            }
        }
    }

    private void showDateTimePicker(final Calendar target, final Runnable onDone) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(target.getTimeInMillis());
        new DatePickerDialog(
            this,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(android.widget.DatePicker view, final int year, final int month, final int dayOfMonth) {
                    new TimePickerDialog(
                        ScheduleActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(android.widget.TimePicker view, int hour, int minute) {
                                target.set(year, month, dayOfMonth, hour, minute, 0);
                                target.set(Calendar.MILLISECOND, 0);
                                onDone.run();
                            }
                        },
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        true
                    ).show();
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void updateButtons(Button startButton, Button endButton, Calendar calStart, Calendar calEnd, DateFormat fmt) {
        startButton.setText(fmt.format(calStart.getTimeInMillis()));
        endButton.setText(fmt.format(calEnd.getTimeInMillis()));
    }
}
