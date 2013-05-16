
package ch.epfl.unison.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * @author marc
 */
public class UnisonDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "ch.epfl.unison.UnisonDBHelper";

    private static final String LIBE_SCHEMA = "CREATE TABLE IF NOT EXISTS "
            + ConstDB.LIBE_TABLE_NAME + " ("
            + ConstDB.C_ID + " integer PRIMARY KEY AUTOINCREMENT, "
            + ConstDB.LIBE_C_LOCAL_ID + " int UNIQUE, "
            + ConstDB.LIBE_C_ARTIST + " text, "
            + ConstDB.LIBE_C_TITLE + " text, "
            + ConstDB.C_IS_CHECKED + " tinyint DEFAULT 0" // used a boolean value
    		+ ")";

    private static final String TAG_SCHEMA = "CREATE TABLE IF NOT EXISTS "
            + ConstDB.TAG_TABLE_NAME + " ("
            + ConstDB.C_ID + " integer PRIMARY KEY AUTOINCREMENT, "
            + ConstDB.TAG_C_NAME + " text UNIQUE NOT NULL, "
            + ConstDB.TAG_C_REMOTE_ID + " bigint UNIQUE, "
            + ConstDB.C_IS_CHECKED + " tinyint DEFAULT 0" // used a boolean value
            + "); "
            + "CREATE INDEX IF NOT EXISTS " + ConstDB.TAG_INDEX_NAME + " ON " + ConstDB.TAG_TABLE_NAME
            + " (" + ConstDB.TAG_C_NAME + ");";

    UnisonDBHelper(Context context, String name, CursorFactory factory, int version) {
        super(context, ConstDB.DATABASE_NAME, null, ConstDB.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(LIBE_SCHEMA);
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
        db.execSQL("DROP TABLE IF EXISTS " + ConstDB.TAG_TABLE_NAME);
        onCreate(db);
    }
}
