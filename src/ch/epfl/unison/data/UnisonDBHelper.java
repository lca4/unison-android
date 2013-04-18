
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

    // private static LinkedList<String> smSchemas = new LinkedList<String>();

    private static final String LIBE_SCHEMA = "CREATE TABLE "
            + Const.LIBE_TABLE_NAME + " ("
            + Const.LIBE_C_ID + " int PRIMARY KEY, "
            + Const.LIBE_C_LOCAL_ID + " int UNIQUE, "
            + Const.LIBE_C_ARTIST + " text, "
            + Const.LIBE_C_TITLE + " text)";

    private static final String TAGS_SCHEMA = "CREATE TABLE "
            + Const.TAGS_TABLE_NAME + " ("
            // TODO
            + " );";

    UnisonDBHelper(Context context, String name, CursorFactory factory, int version) {
        super(context, Const.DATABASE_NAME, null, Const.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // init();
        try {
            db.execSQL(LIBE_SCHEMA);
            db.execSQL(TAGS_SCHEMA);
            // Iterator<String> i = smSchemas.iterator();
            // while (i.hasNext()) {
            // db.execSQL(i.next());
            // }
        } catch (SQLiteException e) {
            Log.v(TAG, e.getMessage()); // "Create table exception"
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        Log.w(TAG, "Upgrading from version " + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + Const.TAGS_TABLE_NAME);
        onCreate(db);
    }

    // private void init() {
    // smSchemas.add(LIBE_SCHEMA);
    // smSchemas.add(TAGS_SCHEMA);
    // }
}
