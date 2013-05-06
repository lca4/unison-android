
package ch.epfl.unison.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

/**
 * @author marc
 */
public class UnisonDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "ch.epfl.unison.UnisonDBHelper";

    private static final String LIBE_SCHEMA = "CREATE TABLE IF NOT EXISTS "
            + Const.LIBE_TABLE_NAME + " ("
            + Const.C_ID + " integer PRIMARY KEY AUTOINCREMENT, "
            + Const.LIBE_C_LOCAL_ID + " int UNIQUE, "
            + Const.LIBE_C_ARTIST + " text, "
            + Const.LIBE_C_TITLE + " text, "
            + Const.C_IS_CHECKED + " tinyint DEFAULT 0" // used a boolean value
    		+ ")";

    private static final String TAG_SCHEMA = "CREATE TABLE IF NOT EXISTS "
            + Const.TAG_TABLE_NAME + " ("
            + Const.C_ID + " integer PRIMARY KEY AUTOINCREMENT, "
            + Const.TAG_C_NAME + " text UNIQUE NOT NULL, "
            + Const.TAG_C_REMOTE_ID + " bigint UNIQUE, "
            + Const.C_IS_CHECKED + " tinyint DEFAULT 0" // used a boolean value
            + "); "
            + "CREATE INDEX IF NOT EXISTS " + Const.TAG_INDEX_NAME + " ON " + Const.TAG_TABLE_NAME
            + " (" + Const.TAG_C_NAME + ");";

    UnisonDBHelper(Context context, String name, CursorFactory factory, int version) {
        super(context, Const.DATABASE_NAME, null, Const.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            Log.i(TAG, "Creates lib_entry table");
            db.execSQL(LIBE_SCHEMA);
            Log.i(TAG, "Creates tag table");
            db.execSQL(TAG_SCHEMA);
        } catch (SQLiteException e) {
            Log.v(TAG, e.getMessage()); // "Create table exception"
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        Log.w(TAG, "Upgrading from version " + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + Const.TAG_TABLE_NAME);
        onCreate(db);
    }
}
