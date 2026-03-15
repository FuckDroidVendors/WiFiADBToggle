package fuck.wifiadbtoggle.droidvendorssuck

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ScheduleDbHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_START INTEGER NOT NULL,
                $COL_END INTEGER NOT NULL,
                $COL_MODE TEXT NOT NULL,
                $COL_ENABLED INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    fun insert(entry: ScheduleEntry): Long {
        val values = ContentValues().apply {
            put(COL_TITLE, entry.title)
            put(COL_START, entry.startMillis)
            put(COL_END, entry.endMillis)
            put(COL_MODE, entry.mode.id)
            put(COL_ENABLED, if (entry.enabled) 1 else 0)
        }
        return writableDatabase.insert(TABLE, null, values)
    }

    fun update(entry: ScheduleEntry) {
        val values = ContentValues().apply {
            put(COL_TITLE, entry.title)
            put(COL_START, entry.startMillis)
            put(COL_END, entry.endMillis)
            put(COL_MODE, entry.mode.id)
            put(COL_ENABLED, if (entry.enabled) 1 else 0)
        }
        writableDatabase.update(TABLE, values, "$COL_ID=?", arrayOf(entry.id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete(TABLE, "$COL_ID=?", arrayOf(id.toString()))
    }

    fun listAll(): List<ScheduleEntry> {
        val db = readableDatabase
        val cursor = db.query(TABLE, null, null, null, null, null, "$COL_START ASC")
        return cursor.use { readCursor(it) }
    }

    fun listForDay(startOfDay: Long, endOfDay: Long): List<ScheduleEntry> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE,
            null,
            "$COL_END>? AND $COL_START<?",
            arrayOf(startOfDay.toString(), endOfDay.toString()),
            null,
            null,
            "$COL_START ASC"
        )
        return cursor.use { readCursor(it) }
    }

    fun hasAnyEnabled(): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE,
            arrayOf(COL_ID),
            "$COL_ENABLED=1",
            null,
            null,
            null,
            null,
            "1"
        )
        return cursor.use { it.moveToFirst() }
    }

    private fun readCursor(cursor: Cursor): List<ScheduleEntry> {
        val items = mutableListOf<ScheduleEntry>()
        val idIdx = cursor.getColumnIndexOrThrow(COL_ID)
        val titleIdx = cursor.getColumnIndexOrThrow(COL_TITLE)
        val startIdx = cursor.getColumnIndexOrThrow(COL_START)
        val endIdx = cursor.getColumnIndexOrThrow(COL_END)
        val modeIdx = cursor.getColumnIndexOrThrow(COL_MODE)
        val enabledIdx = cursor.getColumnIndexOrThrow(COL_ENABLED)
        while (cursor.moveToNext()) {
            items.add(
                ScheduleEntry(
                    id = cursor.getLong(idIdx),
                    title = cursor.getString(titleIdx),
                    startMillis = cursor.getLong(startIdx),
                    endMillis = cursor.getLong(endIdx),
                    mode = ScheduleMode.fromId(cursor.getString(modeIdx)),
                    enabled = cursor.getInt(enabledIdx) == 1
                )
            )
        }
        return items
    }

    companion object {
        private const val DB_NAME = "schedule.db"
        private const val DB_VERSION = 1
        private const val TABLE = "schedules"
        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_START = "start_millis"
        private const val COL_END = "end_millis"
        private const val COL_MODE = "mode"
        private const val COL_ENABLED = "enabled"
    }
}
