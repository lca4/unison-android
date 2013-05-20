
package ch.epfl.unison.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import ch.epfl.unison.Const;

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
            + ConstDB.C_IS_CHECKED + " tinyint DEFAULT 0" // used a boolean
                                                          // value
            + ")";

    private static final String TAG_SCHEMA = "CREATE TABLE IF NOT EXISTS "
            + ConstDB.TAG_TABLE_NAME + " ("
            + ConstDB.C_ID + " integer PRIMARY KEY AUTOINCREMENT, "
            + ConstDB.TAG_C_NAME + " text UNIQUE NOT NULL, "
            + ConstDB.TAG_C_REMOTE_ID + " bigint UNIQUE, "
            + ConstDB.C_IS_CHECKED
            + " tinyint DEFAULT 0" // used a boolean value
            + "); "
            + "CREATE INDEX IF NOT EXISTS " + ConstDB.TAG_INDEX_NAME + " ON "
            + ConstDB.TAG_TABLE_NAME
            + " (" + ConstDB.TAG_C_NAME + ");";

    UnisonDBHelper(Context context, String name, CursorFactory factory, int version) {
        super(context, ConstDB.DATABASE_NAME, null, ConstDB.DATABASE_VERSION);
    }

    private static final String PLYL_SCHEMA = "CREATE TABLE IF NOT EXISTS "
            + ConstDB.PLAYLISTS_TABLE_NAME + " ("
            + ConstDB.C_ID + " integer PRIMARY KEY AUTOINCREMENT, "
            + ConstDB.PLYL_C_LOCAL_ID + " int UNIQUE, "
            + ConstDB.PLYL_C_GS_SIZE + " int, "
            + ConstDB.PLYL_C_CREATED_BY_GS + " tinyint DEFAULT 0, "
            + ConstDB.PLYL_C_GS_ID + " bigint, "
            + ConstDB.PLYL_C_GS_CREATION_TIME + " datetime, " // TODO check
                                                              // type!
            + ConstDB.PLYL_C_GS_UPDATE_TIME + " datetime, " // TODO check type!
            + ConstDB.PLYL_C_GS_AUTHOR_ID + " bigint, "
            + ConstDB.PLYL_C_GS_AUTHOR_NAME + " text, "
            + ConstDB.PLYL_C_GS_AVG_RATING + " double, "
            + ConstDB.PLYL_C_GS_IS_SHARED + " tinyint DEFAULT 0, "
            + ConstDB.PLYL_C_GS_IS_SYNCED + " tinyint DEFAULT 1, "
            + ConstDB.PLYL_C_GS_USER_RATING + " tinyint, "
            + ConstDB.PLYL_C_GS_USER_COMMENT + " text "
            + "); "
            // Create some indexes
            + "CREATE INDEX IF NOT EXISTS " + ConstDB.PLYL_INDEX_LOCAL_ID + " ON "
            + ConstDB.PLAYLISTS_TABLE_NAME + "(" + ConstDB.PLYL_C_LOCAL_ID + ");"
            + "CREATE INDEX IF NOT EXISTS " + ConstDB.PLYL_INDEX_GS_ID + " ON "
            + ConstDB.PLAYLISTS_TABLE_NAME + "(" + ConstDB.PLYL_C_GS_ID + ");"
            + "CREATE INDEX IF NOT EXISTS " + ConstDB.PLYL_INDEX_GS_SIZE + " ON "
            + ConstDB.PLAYLISTS_TABLE_NAME + "(" + ConstDB.PLYL_C_GS_SIZE + ");"
            + "CREATE INDEX IF NOT EXISTS " + ConstDB.PLYL_INDEX_GS_ID + " ON "
            + ConstDB.PLAYLISTS_TABLE_NAME + "(" + ConstDB.PLYL_C_GS_AVG_RATING + ");"
            + "CREATE INDEX IF NOT EXISTS " + ConstDB.PLYL_INDEX_GS_USER_RATING + " ON "
            + ConstDB.PLAYLISTS_TABLE_NAME + "(" + ConstDB.PLYL_C_GS_USER_RATING + ");";

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(LIBE_SCHEMA);
            db.execSQL(TAG_SCHEMA);
            db.execSQL(PLYL_SCHEMA);
        } catch (SQLiteException e) {
            Log.v(TAG, e.getMessage()); // "Create table exception"
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /*
         * WARNING : CRITICAL CODE HERE Erroneous DB updates can lead to
         * irreversible lost of data!
         */
        Log.w(TAG, "Upgrading from version " + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + ConstDB.TAG_TABLE_NAME);
        onCreate(db);
    }
}
