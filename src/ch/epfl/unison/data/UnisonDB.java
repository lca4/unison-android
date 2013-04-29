
package ch.epfl.unison.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import ch.epfl.unison.Const.SeedType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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

    private SQLiteDatabase mDB;
    private final Context mContext;
    private final UnisonDBHelper mDbHelper;

    private static final String LIBE_WHERE_ALL = Const.LIBE_C_LOCAL_ID + " = ? AND "
            + Const.LIBE_C_ARTIST + " = ? AND " + Const.LIBE_C_TITLE + " = ?";
    private static final String TAGS_WHERE_ALL = Const.TAG_C_ID + " = ? AND "
            + Const.TAG_C_NAME + " = ? AND "
            + Const.TAG_C_IS_CHECKED + " = ?";
    // private static final String TAGS_WHERE_REMOTE_ID = Const.TAG_C_REMOTE_ID
    // + " = ?";
    private static final String TAG_WHERE_NAME = Const.TAG_C_NAME + " LIKE ? ";

    // private static final String TAGS_WHERE_C_ID = Const.TAGS_C_ID + " = ?";

    public UnisonDB(Context c) {
        mContext = c;
        mContext.deleteDatabase(Const.DATABASE_NAME);
        mDbHelper = new UnisonDBHelper(mContext, Const.DATABASE_NAME, null, Const.DATABASE_VERSION);
    }

    private void open() {
        Log.i(TAG, "open database in read-only mode");
        mDB = mDbHelper.getReadableDatabase();
    }

    private void openW() {
        Log.i(TAG, "open database in writable mode");
        mDB = mDbHelper.getWritableDatabase();
    }

    private void close() {
        mDB.close();
    }

    private Cursor getCursor(String table, String[] columns) {
        open();
        return mDB.query(table, columns, null, null, null, null, null);
    }

    private Cursor getCursor(String table, String[] columns, String selection,
            String[] selectionArgs) {
        open();
        return mDB.query(table, columns, selection, selectionArgs, null, null, null);
    }

    private Cursor getCursorW(String table, String[] columns) {
        openW();
        return mDB.query(table, columns, null, null, null, null, null);
    }

    private void closeCursor(Cursor openCursor) {
        openCursor.close();
        close();
    }

    private void resetIsChecked(String table) {
        openW();
        ContentValues values = new ContentValues();
        values.put(Const.C_IS_CHECKED, Const.FALSE);
        mDB.update(table, values, null, null);
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
        mDB.insert(Const.LIBE_TABLE_NAME, null, values);
        close();
    }

    public void delete(MusicItem item) {
        openW();
        mDB.delete(Const.LIBE_TABLE_NAME, LIBE_WHERE_ALL,
                new String[] {
                        String.valueOf(item.localId), item.artist, item.title
                });
        close();
    }

    public boolean exists(MusicItem item) {
        open();
        Cursor cur = mDB.query(Const.LIBE_TABLE_NAME,
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
        mDB.delete(Const.LIBE_TABLE_NAME, null, null);
        close();
    }

    public JSONObject libeGetCheckedItems() {
        return getCheckedItems(Const.LIBE_TABLE_NAME, Const.C_IS_CHECKED, SeedType.TRACKS);
    }

    /*
     * Tags specific methods
     */

    /**
     * Don't forget to close the cursor!
     * 
     * @return
     */
    public Cursor tagGetItemsCursor() {
        Cursor cursor = getCursor(Const.TAG_TABLE_NAME, new String[] {
                Const.TAG_C_ID, Const.TAG_C_NAME, Const.C_IS_CHECKED
        });
        return cursor;
    }

    public void tagSetChecked(int tagId, boolean isChecked) {
        openW();
        ContentValues values = new ContentValues();
        if (isChecked) {
            values.put(Const.C_IS_CHECKED, Const.TRUE);
        } else {
            values.put(Const.C_IS_CHECKED, Const.FALSE);
        }
        Log.i(TAG, "updates row id=" + tagId + " to is_checked=" + values.valueSet().toString());
        mDB.update(Const.TAG_TABLE_NAME, values, "_id = ? ", new String[] {
                String.valueOf(tagId)
        });
        close();
    }

    public void tagSetChecked(LinkedHashMap<String, Integer> items, boolean[] checked) {
        openW();
        ContentValues values = new ContentValues();
        Set<String> keys = items.keySet();
        Iterator<String> it = keys.iterator();
        int index = 0;
        while (it.hasNext()) {
            if (checked[index]) {
                values.put(Const.C_IS_CHECKED, Const.TRUE);
            } else {
                values.put(Const.C_IS_CHECKED, Const.FALSE);
            }
            int tagId = items.get(it);
            Log.i(TAG, "Updates row id=" + tagId + " to is_checked=" + values.valueSet().toString());
            mDB.update(Const.TAG_TABLE_NAME, values, "_id = ? ", new String[] {
                    String.valueOf(tagId)
            });
            index++;
        }
        close();
    }

    public LinkedHashMap<String, Integer> getTags() {
        Cursor cursor = getCursor(Const.TAG_TABLE_NAME, new String[] {
                Const.TAG_C_ID, Const.TAG_C_NAME
        });
        LinkedHashMap<String, Integer> tags = null;
        // List<CharSequence> tags = null;
        if (cursor != null && cursor.moveToFirst()) {
            tags = new LinkedHashMap<String, Integer>();
            int colId = cursor.getColumnIndex(Const.C_ID);
            int colName = cursor.getColumnIndex(Const.TAG_C_NAME);
            do {
                tags.put(cursor.getString(colName), cursor.getInt(colId));
            } while (cursor.moveToNext());
        }
        closeCursor(cursor);
        // return tags.toArray(new CharSequence[tags.size()]);
        return tags;
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

    public Set<TagItem> tagGetItems() {
        Cursor cur = getCursor(Const.TAG_TABLE_NAME,
                new String[] {
                        Const.TAG_C_ID, Const.TAG_C_NAME
                });
        Set<TagItem> set = new HashSet<TagItem>();
        if (cur != null && cur.moveToFirst()) {
            int colId = cur.getColumnIndex(Const.TAG_C_ID);
            int colName = cur.getColumnIndex(Const.TAG_C_NAME);
            int colIsChecked = cur.getColumnIndex(Const.TAG_C_IS_CHECKED);
            int colRemoteId = cur.getColumnIndex(Const.TAG_C_REMOTE_ID);
            do {
                set.add(new TagItem(cur.getInt(colId),
                        cur.getString(colName), cur.getInt(colIsChecked),
                        cur.getLong(colRemoteId)));
            } while (cur.moveToNext());
        }
        closeCursor(cur);
        return set;
    }

    public JSONObject tagGetCheckedItems() {
        return getCheckedItems(Const.TAG_TABLE_NAME, Const.C_IS_CHECKED, SeedType.TAGS);
    }

    private JSONObject getCheckedItems(String table, String selection, SeedType key) {
        JSONObject json = null;
        Cursor cur = getCursor(table, new String[] {
                selection
        }, Const.C_IS_CHECKED + " = ? ", new String[] {
                String.valueOf(Const.TRUE)
        });
        if (cur != null && cur.moveToFirst()) {
            json = new JSONObject();
            int colName = cur.getColumnIndex(selection);
            do {
                try {
                    json.put(key.getLabel(), cur.getString(colName));
                    Log.i(TAG, "added seed: " + cur.getString(colName) + " to " + key.getLabel());
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    Log.i(TAG, e.getMessage());
                }
            } while (cur.moveToNext());
        }
        closeCursor(cur);
        resetIsChecked(table);
        return json;
    }

    public boolean tagIsEmpty() {
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
            long newid = mDB.insert(Const.TAG_TABLE_NAME, null, values);
            Log.i(TAG, "new inserted tag id: " + newid);
            close();
        }
    }

    public void delete(TagItem item) {
        openW();
        mDB.delete(Const.TAG_TABLE_NAME, TAGS_WHERE_ALL,
                new String[] {
                        String.valueOf(item.localId), item.name
                });
        close();
    }

    public boolean exists(TagItem item) {
        open();
        Cursor cur = mDB.query(Const.TAG_TABLE_NAME,
                new String[] {
                        Const.TAG_C_NAME
                },
                TAG_WHERE_NAME,
                new String[] {
                    item.name
                },
                null, null, null, "1"); // LIMIT 1
        boolean exists = cur.moveToFirst();
        closeCursor(cur);
        return exists;
    }

    public String getColumnLabelRowId() {
        return Const.C_ID;
    }

    public String tagGetColumnLabelIsChecked() {
        return Const.C_IS_CHECKED;
    }

    public String tagGetColumnLabelName() {
        return Const.TAG_C_NAME;
    }

    public void tagEmpty() {
        openW();
        mDB.delete(Const.TAG_TABLE_NAME, null, null);
        close();
    }
}
