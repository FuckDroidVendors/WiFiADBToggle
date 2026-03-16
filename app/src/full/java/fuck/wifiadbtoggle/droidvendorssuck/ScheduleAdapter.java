package fuck.wifiadbtoggle.droidvendorssuck;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {
    public interface OnClickListener {
        void onClick(ScheduleEntry entry);
    }

    private final OnClickListener onClick;
    private final List<ScheduleEntry> items = new ArrayList<>();
    private final DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

    public ScheduleAdapter(OnClickListener onClick) {
        this.onClick = onClick;
    }

    public void submit(List<ScheduleEntry> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view, onClick);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(items.get(position), dateTimeFormat);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView time;
        private final TextView mode;
        private final OnClickListener onClick;

        public ViewHolder(View itemView, OnClickListener onClick) {
            super(itemView);
            this.onClick = onClick;
            title = itemView.findViewById(R.id.scheduleTitle);
            time = itemView.findViewById(R.id.scheduleTime);
            mode = itemView.findViewById(R.id.scheduleMode);
        }

        public void bind(final ScheduleEntry entry, DateFormat fmt) {
            title.setText(entry.title);
            String timeText = fmt.format(entry.startMillis) + " - " + fmt.format(entry.endMillis);
            time.setText(timeText);
            String modeText;
            switch (entry.mode) {
                case FORCE_ON:
                    modeText = itemView.getContext().getString(R.string.schedule_mode_force_on);
                    break;
                case FORCE_OFF:
                    modeText = itemView.getContext().getString(R.string.schedule_mode_force_off);
                    break;
                case RESPECT:
                default:
                    modeText = itemView.getContext().getString(R.string.schedule_mode_respect);
                    break;
            }
            if (entry.enabled) {
                mode.setText(modeText);
            } else {
                mode.setText(modeText + itemView.getContext().getString(R.string.schedule_disabled_suffix));
            }
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClick.onClick(entry);
                }
            });
        }
    }
}
