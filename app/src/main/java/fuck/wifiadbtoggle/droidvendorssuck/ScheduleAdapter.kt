package fuck.wifiadbtoggle.droidvendorssuck

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat

class ScheduleAdapter(
    private val onClick: (ScheduleEntry) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    private val items = mutableListOf<ScheduleEntry>()
    private val dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    fun submit(list: List<ScheduleEntry>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], dateTimeFormat)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View, private val onClick: (ScheduleEntry) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.scheduleTitle)
        private val time: TextView = itemView.findViewById(R.id.scheduleTime)
        private val mode: TextView = itemView.findViewById(R.id.scheduleMode)

        fun bind(entry: ScheduleEntry, fmt: DateFormat) {
            title.text = entry.title
            time.text = "${fmt.format(entry.startMillis)} - ${fmt.format(entry.endMillis)}"
            val modeText = when (entry.mode) {
                ScheduleMode.FORCE_ON -> itemView.context.getString(R.string.schedule_mode_force_on)
                ScheduleMode.FORCE_OFF -> itemView.context.getString(R.string.schedule_mode_force_off)
                ScheduleMode.RESPECT -> itemView.context.getString(R.string.schedule_mode_respect)
            }
            mode.text = if (entry.enabled) {
                modeText
            } else {
                modeText + itemView.context.getString(R.string.schedule_disabled_suffix)
            }
            itemView.setOnClickListener { onClick(entry) }
        }
    }
}
