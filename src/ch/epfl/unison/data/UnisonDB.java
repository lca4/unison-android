
package ch.epfl.unison.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import ch.epfl.unison.MusicItem;

import java.util.HashSet;
import java.util.Set;

/**
 * Class for accessing / managing the unison database. Note: we are talking
 * about the one *that GroupStreamer owns*, i.e. on the phone
 * 
 * @author marc
 */
public class UnisonDB {

    private static final String TAG = "ch.epfl.unison.UnisonDB";

    private SQLiteDatabase mDb;
    private final Context mContext;
    private final UnisonDBHelper mDbHelper;

    private static final String LIBE_WHERE_ALL = Const.LIBE_C_LOCAL_ID + " = ? AND "
            + Const.LIBE_C_ARTIST + " = ? AND " + Const.LIBE_C_TITLE + " = ?";

    public UnisonDB(Context c) {
        mContext = c;
        mDbHelper = new UnisonDBHelper(mContext, Const.DATABASE_NAME, null, Const.DATABASE_VERSION);
    }

    public void open() {
        try {
            mDb = mDbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            Log.v(TAG, e.getMessage()); // "Open database exception caught"
            mDb = mDbHelper.getReadableDatabase();
        }
    }

    public void close() {
        mDb.close();
    }

    /*
     * lib_entry specific methods
     */
    public Set<MusicItem> libeGetEntries() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cur = db.query(Const.LIBE_TABLE_NAME,
                new String[] {
                        Const.LIBE_C_LOCAL_ID, Const.LIBE_C_ARTIST, Const.LIBE_C_TITLE
                },
                null, null, null, null, null);
        Set<MusicItem> set = new HashSet<MusicItem>();
        if (cur != null && cur.moveToFirst()) {
            int colId = cur.getColumnIndex(Const.LIBE_C_LOCAL_ID);
            int colArtist = cur.getColumnIndex(Const.LIBE_C_ARTIST);
            int colTitle = cur.getColumnIndex(Const.LIBE_C_TITLE);
            do {
                set.add(new MusicItem(cur.getInt(colId),
                        cur.getString(colArtist), cur.getString(colTitle)));
            } while (cur.moveToNext());
        }
        cur.close();
        db.close();
        return set;
    }

    public boolean libeIsEmpty() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cur = db.query(Const.LIBE_TABLE_NAME,
                new String[] {
                        Const.LIBE_C_LOCAL_ID, Const.LIBE_C_ARTIST, Const.LIBE_C_TITLE
                },
                null, null, null, null, null);
        boolean isEmpty = !cur.moveToFirst();
        cur.close();
        db.close();
        return isEmpty;
    }

    public void libeInsert(MusicItem item) {
        ContentValues values = new ContentValues();
        values.put(Const.LIBE_C_LOCAL_ID, item.localId);
        values.put(Const.LIBE_C_ARTIST, item.artist);
        values.put(Const.LIBE_C_TITLE, item.title);

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.insert(Const.LIBE_TABLE_NAME, null, values);
        db.close();
    }

    public void libeDelete(MusicItem item) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(Const.LIBE_TABLE_NAME, LIBE_WHERE_ALL,
                new String[] {
                        String.valueOf(item.localId), item.artist, item.title
                });
        db.close();
    }

    public boolean libeExists(MusicItem item) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor c = db.query(Const.LIBE_TABLE_NAME,
                new String[] {
                    Const.LIBE_C_LOCAL_ID
                },
                LIBE_WHERE_ALL,
                new String[] {
                        String.valueOf(item.localId), item.artist, item.title
                },
                null, null, null, "1"); // LIMIT 1
        boolean exists = c.moveToFirst();
        c.close();
        db.close();
        return exists;
    }

    public void libeTruncate() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(Const.LIBE_TABLE_NAME, null, null);
        db.close();
    }

    /*
     * tags specific methods
     */

    public Cursor tagsGetEntries() {
        return null;
    }
}
