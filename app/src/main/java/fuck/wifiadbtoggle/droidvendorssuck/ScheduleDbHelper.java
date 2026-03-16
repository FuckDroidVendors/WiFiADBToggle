package fuck.wifiadbtoggle.droidvendorssuck;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class ScheduleDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "schedule.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "schedules";
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_START = "start_millis";
    private static final String COL_END = "end_millis";
    private static final String COL_MODE = "mode";
    private static final String COL_ENABLED = "enabled";

    public ScheduleDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
            "CREATE TABLE " + TABLE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT NOT NULL, " +
                COL_START + " INTEGER NOT NULL, " +
                COL_END + " INTEGER NOT NULL, " +
                COL_MODE + " TEXT NOT NULL, " +
                COL_ENABLED + " INTEGER NOT NULL" +
            ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public long insert(ScheduleEntry entry) {
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, entry.title);
        values.put(COL_START, entry.startMillis);
        values.put(COL_END, entry.endMillis);
        values.put(COL_MODE, entry.mode.id);
        values.put(COL_ENABLED, entry.enabled ? 1 : 0);
        return getWritableDatabase().insert(TABLE, null, values);
    }

    public void update(ScheduleEntry entry) {
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, entry.title);
        values.put(COL_START, entry.startMillis);
        values.put(COL_END, entry.endMillis);
        values.put(COL_MODE, entry.mode.id);
        values.put(COL_ENABLED, entry.enabled ? 1 : 0);
        getWritableDatabase().update(TABLE, values, COL_ID + "=?", new String[] { Long.toString(entry.id) });
    }

    public void delete(long id) {
        getWritableDatabase().delete(TABLE, COL_ID + "=?", new String[] { Long.toString(id) });
    }

    public List<ScheduleEntry> listAll() {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE, null, null, null, null, null, COL_START + " ASC")) {
            return readCursor(cursor);
        }
    }

    public List<ScheduleEntry> listForDay(long startOfDay, long endOfDay) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(
            TABLE,
            null,
            COL_END + ">? AND " + COL_START + "<?",
            new String[] { Long.toString(startOfDay), Long.toString(endOfDay) },
            null,
            null,
            COL_START + " ASC"
        )) {
            return readCursor(cursor);
        }
    }

    public boolean hasAnyEnabled() {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(
            TABLE,
            new String[] { COL_ID },
            COL_ENABLED + "=1",
            null,
            null,
            null,
            null,
            "1"
        )) {
            return cursor.moveToFirst();
        }
    }

    private List<ScheduleEntry> readCursor(Cursor cursor) {
        List<ScheduleEntry> items = new ArrayList<>();
        int idIdx = cursor.getColumnIndexOrThrow(COL_ID);
        int titleIdx = cursor.getColumnIndexOrThrow(COL_TITLE);
        int startIdx = cursor.getColumnIndexOrThrow(COL_START);
        int endIdx = cursor.getColumnIndexOrThrow(COL_END);
        int modeIdx = cursor.getColumnIndexOrThrow(COL_MODE);
        int enabledIdx = cursor.getColumnIndexOrThrow(COL_ENABLED);
        while (cursor.moveToNext()) {
            items.add(new ScheduleEntry(
                cursor.getLong(idIdx),
                cursor.getString(titleIdx),
                cursor.getLong(startIdx),
                cursor.getLong(endIdx),
                ScheduleMode.fromId(cursor.getString(modeIdx)),
                cursor.getInt(enabledIdx) == 1
            ));
        }
        return items;
    }
}
