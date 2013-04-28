
package ch.epfl.unison.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private static final String TAGS_WHERE_ALL = Const.TAG_C_ID + " = ? AND "
            + Const.TAG_C_NAME + " = ? AND "
            + Const.TAG_C_IS_CHECKED + " = ?";
    // private static final String TAGS_WHERE_REMOTE_ID = Const.TAG_C_REMOTE_ID
    // + " = ?";
    private static final String TAGS_WHERE_NAME = Const.TAG_C_NAME + " = ?";

    // private static final String TAGS_WHERE_C_ID = Const.TAGS_C_ID + " = ?";

    public UnisonDB(Context c) {
        mContext = c;
        mContext.deleteDatabase(Const.DATABASE_NAME);
        mDbHelper = new UnisonDBHelper(mContext, Const.DATABASE_NAME, null, Const.DATABASE_VERSION);
    }

    public void open() {
        Log.i(TAG, "open database in read-only mode");
        mDb = mDbHelper.getReadableDatabase();
    }

    public void openW() {
        Log.i(TAG, "open database in writable mode");
        mDb = mDbHelper.getWritableDatabase();
    }

    public void close() {
        mDb.close();
    }

    private Cursor getCursor(String table, String[] columns) {
        open();
        return mDb.query(table, columns, null, null, null, null, null);
    }

    public void closeCursor(Cursor openCursor) {
        openCursor.close();
        close();
    }

    /*
     * lib_entry specific methods
     */
    public Set<MusicItem> getMusicItems() {
        Cursor cur = getCursor(Const.LIBE_TABLE_NAME,
                new String[] {
                        Const.LIBE_C_LOCAL_ID, Const.LIBE_C_ARTIST, Const.LIBE_C_TITLE
                });
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
        closeCursor(cur);
        return set;
    }

    public boolean libeIsEmpty() {
        Cursor cur = getCursor(Const.LIBE_TABLE_NAME,
                new String[] {
                        Const.LIBE_C_LOCAL_ID, Const.LIBE_C_ARTIST, Const.LIBE_C_TITLE
                });
        boolean isEmpty = !cur.moveToFirst();
        closeCursor(cur);
        return isEmpty;
    }

    public void insert(MusicItem item) {
        ContentValues values = new ContentValues();
        values.put(Const.LIBE_C_LOCAL_ID, item.localId);
        values.put(Const.LIBE_C_ARTIST, item.artist);
        values.put(Const.LIBE_C_TITLE, item.title);

        openW();
        mDb.insert(Const.LIBE_TABLE_NAME, null, values);
        close();
    }

    public void delete(MusicItem item) {
        openW();
        mDb.delete(Const.LIBE_TABLE_NAME, LIBE_WHERE_ALL,
                new String[] {
                        String.valueOf(item.localId), item.artist, item.title
                });
        close();
    }

    public boolean exists(MusicItem item) {
        open();
        Cursor cur = mDb.query(Const.LIBE_TABLE_NAME,
                new String[] {
                    Const.LIBE_C_LOCAL_ID
                },
                LIBE_WHERE_ALL,
                new String[] {
                        String.valueOf(item.localId), item.artist, item.title
                },
                null, null, null, "1"); // LIMIT 1
        boolean exists = cur.moveToFirst();
        closeCursor(cur);
        return exists;
    }

    public void libeTruncate() {
        openW();
        mDb.delete(Const.LIBE_TABLE_NAME, null, null);
        close();
    }

    /*
     * Tags specific methods
     */

    /**
     * Don't forget to close the cursor!
     * 
     * @return
     */
    public Cursor getTagItemsCursor() {
        open();
        Cursor cursor = mDb.query(Const.TAG_TABLE_NAME, new String[] {
                Const.TAG_C_ID, Const.TAG_C_NAME, Const.TAG_C_IS_CHECKED
        },
                null, null, null, null, null);
        return cursor;
    }

    public CharSequence[] getTags() {
        Cursor cursor = getCursor(Const.TAG_TABLE_NAME, new String[] {
                Const.TAG_C_ID, Const.TAG_C_NAME
        });
        List<CharSequence> tags = null;
        if (cursor != null && cursor.moveToFirst()) {
            tags = new ArrayList<CharSequence>();
            // int colId = cursor.getColumnIndex(Const.TAGS_C_ID);
            int colName = cursor.getColumnIndex(Const.TAG_C_NAME);
            do {
                tags.add(cursor.getString(colName));
            } while (cursor.moveToNext());
        }
        closeCursor(cursor);
        return tags.toArray(new CharSequence[tags.size()]);
    }

    // public TagItem getTagItem(int index) {
    // open();
    // Cursor c = mDb.query(Const.TAGS_TABLE_NAME,
    // new String[] {
    // Const.TAGS_C_ID, Const.TAGS_C_NAME, Const.TAGS_C_REMOTE_ID
    // },
    // TAGS_WHERE_C_ID,
    // new String[] {
    // String.valueOf(index)
    // },
    // null, null, null, "1");
    // // TODO some stuff
    // return null;
    // }

    public Set<TagItem> getTagItems() {
        Cursor cur = getCursor(Const.TAG_TABLE_NAME,
                new String[] {
                        Const.TAG_C_ID, Const.TAG_C_NAME
                });
        Set<TagItem> set = new HashSet<TagItem>();
        if (cur != null && cur.moveToFirst()) {
            int colId = cur.getColumnIndex(Const.TAG_C_ID);
            int colName = cur.getColumnIndex(Const.TAG_C_NAME);
            int colRemoteId = cur.getColumnIndex(Const.TAG_C_REMOTE_ID);
            do {
                set.add(new TagItem(cur.getInt(colId),
                        cur.getString(colName), cur.getLong(colRemoteId)));
            } while (cur.moveToNext());
        }
        closeCursor(cur);
        return set;
    }

    public boolean tagsIsEmpty() {
        Cursor cur = getCursor(Const.TAG_TABLE_NAME,
                new String[] {
                        Const.TAG_C_ID, Const.TAG_C_NAME
                });
        boolean isEmpty = !cur.moveToFirst();
        closeCursor(cur);
        return isEmpty;
    }

    /**
     * Inserts the item only if it still does not exists.
     * 
     * @param item
     */
    public void insert(TagItem item) {
        Log.i(TAG, "insert");
        ContentValues values = new ContentValues();
        // values.put(Const.TAGS_C_ID, item.localId);
        values.put(Const.TAG_C_NAME, item.name);
        // values.put(Const.TAG_C_REMOTE_ID, item.remoteId);

        if (!exists(item)) {
            openW();
            long newid = mDb.insert(Const.TAG_TABLE_NAME, null, values);
            Log.i(TAG, "new inserted tag id: " + newid);
            close();
        }
    }

    public void delete(TagItem item) {
        openW();
        mDb.delete(Const.TAG_TABLE_NAME, TAGS_WHERE_ALL,
                new String[] {
                        String.valueOf(item.localId), item.name
                });
        close();
    }

    public boolean exists(TagItem item) {
        open();
        Cursor cur = mDb.query(Const.TAG_TABLE_NAME,
                new String[] {
                    Const.TAG_C_ID
                },
                TAGS_WHERE_NAME,
                new String[] {
                    String.valueOf(item.name)
                },
                null, null, null, "1"); // LIMIT 1
        boolean exists = cur.moveToFirst();
        closeCursor(cur);
        return exists;
    }

    public String getTagIsCheckedColumnLabel() {
        return Const.TAG_C_IS_CHECKED;
    }

    public String getTagNameColumnLabel() {
        return Const.TAG_C_NAME;
    }

    public void emptyTags() {
        openW();
        mDb.delete(Const.TAG_TABLE_NAME, null, null);
        close();
    }
}
