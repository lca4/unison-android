
package ch.epfl.unison.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import ch.epfl.unison.Const.SeedType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class for accessing / managing the unison database. Note: we are talking
 * about the one *that GroupStreamer owns*, i.e. on the phone <br />
 * <br />
 * Refactoring in progress:
 * <ul>
 * <li>1 private inner class for each table</li>
 * <li>interface implemented by every such inner classes</li>
 * </ul>
 * 
 * @author marc
 */
public class UnisonDB {

    private static final String TAG = "ch.epfl.unison.UnisonDB";

    private SQLiteDatabase mDB;
    private final Context mContext;
    private final UnisonDBHelper mDbHelper;

    private static final String LIBE_WHERE_ALL = ConstDB.LIBE_C_LOCAL_ID + " = ? AND "
            + ConstDB.LIBE_C_ARTIST + " = ? AND " + ConstDB.LIBE_C_TITLE + " = ?";
    private static final String TAGS_WHERE_ALL = ConstDB.C_ID + " = ? AND "
            + ConstDB.TAG_C_NAME + " = ? AND "
            + ConstDB.C_IS_CHECKED + " = ?";
    // private static final String TAGS_WHERE_REMOTE_ID = Const.TAG_C_REMOTE_ID
    // + " = ?";
    private static final String TAG_WHERE_NAME = ConstDB.TAG_C_NAME + " LIKE ? ";

    // private static final String TAGS_WHERE_C_ID = Const.TAGS_C_ID + " = ?";

    public UnisonDB(Context c) {
        mContext = c;
        // Log.e(TAG + "UnisonDB", "REMOVE THE DB DELETION ON PROD APP");
        // mContext.deleteDatabase(ConstDB.DATABASE_NAME); // TODO remove for
        // production app!
        mDbHelper = new UnisonDBHelper(mContext, ConstDB.DATABASE_NAME, null,
                ConstDB.DATABASE_VERSION);
    }

    private void open() {
        mDB = mDbHelper.getReadableDatabase();
    }

    private SQLiteDatabase openW() {
        mDB = mDbHelper.getWritableDatabase();
        return mDB;
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
        if (openCursor != null) {
            openCursor.close();
        }
        close();
    }

    private void resetIsChecked(String table) {
        openW();
        ContentValues values = new ContentValues();
        values.put(ConstDB.C_IS_CHECKED, ConstDB.FALSE);
        mDB.update(table, values, null, null);
        close();
    }
    
    private interface ITableHandler {
        boolean isEmpty();
        boolean exists(AbstractItem i);
        void insert(AbstractItem i);
        AbstractItem getItem(long index);
        Set<AbstractItem> getItems();
        int delete(AbstractItem i);
        void truncate();
    }

    /**
     * @param itemType
     * @return
     * @throws #{@link IllegalArgumentException} if type not supported
     */
    public Set<?> getEntries(Class<?> itemType) {
        if (itemType == MusicItem.class) {
            return getMusicItems();
        } else if (itemType == TagItem.class) {
            return getTagItems();
        } else if (itemType == PlaylistItem.class) {
            return getPlylItems();
        } else {
            // Unsupported type
            throw new IllegalArgumentException();
        }
    }

    private Set<PlaylistItem> getPlylItems() {
        Cursor cur = getCursor(ConstDB.PLAYLISTS_TABLE_NAME,
                new String[] {
                        ConstDB.PLYL_C_LOCAL_ID,
                        ConstDB.PLYL_C_GS_SIZE,
                        ConstDB.PLYL_C_CREATED_BY_GS,
                        ConstDB.PLYL_C_GS_ID,
                        ConstDB.PLYL_C_GS_CREATION_TIME,
                        ConstDB.PLYL_C_GS_UPDATE_TIME,
                        ConstDB.PLYL_C_GS_AUTHOR_ID,
                        ConstDB.PLYL_C_GS_AUTHOR_NAME,
                        ConstDB.PLYL_C_GS_AVG_RATING,
                        ConstDB.PLYL_C_GS_IS_SHARED,
                        ConstDB.PLYL_C_GS_IS_SYNCED,
                        ConstDB.PLYL_C_GS_USER_RATING,
                        ConstDB.PLYL_C_GS_USER_COMMENT
                });
        Set<PlaylistItem> set = new HashSet<PlaylistItem>();
        if (cur != null && cur.moveToFirst()) {
            int colLocalId = cur.getColumnIndex(ConstDB.PLYL_C_LOCAL_ID);
            int colGSSize = cur.getColumnIndex(ConstDB.PLYL_C_GS_SIZE);
            int colCreatedByGS = cur.getColumnIndex(ConstDB.PLYL_C_CREATED_BY_GS);
            int colGSId = cur.getColumnIndex(ConstDB.PLYL_C_GS_ID);
            int colGSCreationTime = cur.getColumnIndex(ConstDB.PLYL_C_GS_CREATION_TIME);
            int colGSUpdateTime = cur.getColumnIndex(ConstDB.PLYL_C_GS_UPDATE_TIME);
            int colGSAuthorId = cur.getColumnIndex(ConstDB.PLYL_C_GS_AUTHOR_ID);
            int colGSAuthorName = cur.getColumnIndex(ConstDB.PLYL_C_GS_AUTHOR_NAME);
            int colGSAvgRating = cur.getColumnIndex(ConstDB.PLYL_C_GS_AVG_RATING);
            int colGSIsShared = cur.getColumnIndex(ConstDB.PLYL_C_GS_IS_SHARED);
            int colGSIsSynced = cur.getColumnIndex(ConstDB.PLYL_C_GS_IS_SYNCED);
            int colGSUserRating = cur.getColumnIndex(ConstDB.PLYL_C_GS_USER_RATING);
            int colGSUserComment = cur.getColumnIndex(ConstDB.PLYL_C_GS_USER_COMMENT);
            do {
                set.add(new PlaylistItem.Builder().localId(cur.getInt(colLocalId))
                        .size(cur.getInt(colGSSize))
                        .createdByGS(cur.getInt(colCreatedByGS) != 0)
                        .plId(cur.getInt(colGSId))
                        .created(cur.getString(colGSCreationTime))
                        .gsUpdated(cur.getString(colGSUpdateTime))
                        .authorId(cur.getInt(colGSAuthorId))
                        .authorName(cur.getString(colGSAuthorName))
                        .avgRating(cur.getDouble(colGSAvgRating))
                        .isShared(cur.getInt(colGSIsShared) != 0)
                        .isSynced(cur.getInt(colGSIsSynced) != 0)
                        .userRating(cur.getInt(colGSUserRating))
                        .userComment(cur.getString(colGSUserComment))
                        .build());
            } while (cur.moveToNext());
        }
        closeCursor(cur);
        return set;
    }

    public boolean isEmpty(Class<?> itemType) {
        String table = null;
        if (itemType == MusicItem.class) {
            table = ConstDB.LIBE_TABLE_NAME;
        } else if (itemType == TagItem.class) {
            table = ConstDB.TAG_TABLE_NAME;
        } else if (itemType == PlaylistItem.class) {
            table = ConstDB.PLAYLISTS_TABLE_NAME;
        } else {
            // Unsupported type
            throw new IllegalArgumentException();
        }
        Cursor cur = getCursor(table,
                new String[] {
                    ConstDB.C_ID
                });
        boolean isEmpty = !cur.moveToFirst();
        closeCursor(cur);
        return isEmpty;
    }

    public void truncate(Class<?> itemType) {
        if (itemType == MusicItem.class) {
            libeTruncate();
        } else if (itemType == TagItem.class) {
            tagTruncate();
        } else if (itemType == PlaylistItem.class) {
            plylTruncate();
        } else {
            // Unsupported type
            throw new IllegalArgumentException();
        }
    }

    public boolean exists(Class<?> item) {
        // TODO
        return false;
    }

    public int delete(Object item) {
        if (item instanceof MusicItem) {
            return delete((MusicItem) item);
        } else if (item instanceof TagItem) {
            return delete((TagItem) item);
        } else if (item instanceof PlaylistItem) {
            return delete((PlaylistItem) item);
        } else {
            // Unsupported type
            throw new IllegalArgumentException();
        }
    }

    /**
     * @param itemType
     * @param itemId
     * @return if found, the object in type itemType; null else.
     */
    public AbstractItem getItem(Class<?> itemType, long itemId) {
        AbstractItem res = null;
        if (itemType == MusicItem.class) {
            // Not yet implemented
            res = null;
        } else if (itemType == TagItem.class) {
            // Not yet implemented
            res = null;
        } else if (itemType == PlaylistItem.class) {
            res = plylGetItem(itemId);
        } else {
            // Unsupported type
            throw new IllegalArgumentException();
        }
        return res;
    }

    public boolean isMadeWithGS(long id) {
        Cursor cur = getCursor(ConstDB.PLAYLISTS_TABLE_NAME,
                new String[] {
                        ConstDB.PLYL_C_LOCAL_ID,
                        ConstDB.PLYL_C_CREATED_BY_GS
                }, ConstDB.PLYL_C_LOCAL_ID + " = ? ", new String[] {
                    String.valueOf(id)
                });
        boolean result = false;
        if (cur != null && cur.getCount() == 1 && cur.moveToFirst()) {
            int colCreatedByGS = cur.getColumnIndex(ConstDB.PLYL_C_CREATED_BY_GS);
            if (cur.getInt(colCreatedByGS) > 0) {
                result = true;
            }
        }
        closeCursor(cur);
        return result;
    }
    
    /**
     * Was the playlist plid made by the user uid with GS?
     * 
     * @param plid
     * @param uid
     * @return
     */
    public boolean isMadeWithGS(long plid, long uid) {
        String selection = ConstDB.PLYL_C_LOCAL_ID + " = ? "
                + "AND " + ConstDB.PLYL_C_CREATED_BY_GS + " = ? "
                + "AND " + ConstDB.PLYL_C_GS_AUTHOR_ID + " = ? ";
        Cursor cur = getCursor(ConstDB.PLAYLISTS_TABLE_NAME,
                new String[] {
                        ConstDB.PLYL_C_LOCAL_ID,
                        ConstDB.PLYL_C_CREATED_BY_GS,
                        ConstDB.PLYL_C_GS_AUTHOR_ID
                }, selection, new String[] {
                    String.valueOf(plid),
                    String.valueOf(ConstDB.TRUE),
                    String.valueOf(uid)
                });
        boolean result = false;
        if (cur != null && cur.moveToFirst()) {
            result = true;
        }
        closeCursor(cur);
        return result;
    }

    public int getTracksCount(long playlistId) {
        return AndroidDB.getTracksCount(mContext.getContentResolver(), playlistId);
    }

    private PlaylistItem plylGetItem(long id) {
        Cursor cur = getCursor(ConstDB.PLAYLISTS_TABLE_NAME,
                new String[] {
                        ConstDB.PLYL_C_LOCAL_ID,
                        ConstDB.PLYL_C_GS_SIZE,
                        ConstDB.PLYL_C_CREATED_BY_GS,
                        ConstDB.PLYL_C_GS_ID,
                        ConstDB.PLYL_C_GS_CREATION_TIME,
                        ConstDB.PLYL_C_GS_UPDATE_TIME,
                        ConstDB.PLYL_C_GS_AUTHOR_ID,
                        ConstDB.PLYL_C_GS_AUTHOR_NAME,
                        ConstDB.PLYL_C_GS_AVG_RATING,
                        ConstDB.PLYL_C_GS_IS_SHARED,
                        ConstDB.PLYL_C_GS_IS_SYNCED,
                        ConstDB.PLYL_C_GS_USER_RATING,
                        ConstDB.PLYL_C_GS_USER_COMMENT
                }, ConstDB.PLYL_C_LOCAL_ID + " = ? ", new String[] {
                    String.valueOf(id)
                });
        PlaylistItem pl = null;
        if (cur != null && cur.moveToFirst()) {
            int colLocalId = cur.getColumnIndex(ConstDB.PLYL_C_LOCAL_ID);
            int colGSSize = cur.getColumnIndex(ConstDB.PLYL_C_GS_SIZE);
            int colCreatedByGS = cur.getColumnIndex(ConstDB.PLYL_C_CREATED_BY_GS);
            int colGSId = cur.getColumnIndex(ConstDB.PLYL_C_GS_ID);
            int colGSCreationTime = cur.getColumnIndex(ConstDB.PLYL_C_GS_CREATION_TIME);
            int colGSUpdateTime = cur.getColumnIndex(ConstDB.PLYL_C_GS_UPDATE_TIME);
            int colGSAuthorId = cur.getColumnIndex(ConstDB.PLYL_C_GS_AUTHOR_ID);
            int colGSAuthorName = cur.getColumnIndex(ConstDB.PLYL_C_GS_AUTHOR_NAME);
            int colGSAvgRating = cur.getColumnIndex(ConstDB.PLYL_C_GS_AVG_RATING);
            int colGSIsShared = cur.getColumnIndex(ConstDB.PLYL_C_GS_IS_SHARED);
            int colGSIsSynced = cur.getColumnIndex(ConstDB.PLYL_C_GS_IS_SYNCED);
            int colGSUserRating = cur.getColumnIndex(ConstDB.PLYL_C_GS_USER_RATING);
            int colGSUserComment = cur.getColumnIndex(ConstDB.PLYL_C_GS_USER_COMMENT);
            pl = new PlaylistItem.Builder().localId(cur.getInt(colLocalId))
                    .size(cur.getInt(colGSSize))
                    .createdByGS(cur.getInt(colCreatedByGS) != 0)
                    .plId(cur.getInt(colGSId))
                    .created(cur.getString(colGSCreationTime))
                    .gsUpdated(cur.getString(colGSUpdateTime))
                    .authorId(cur.getInt(colGSAuthorId))
                    .authorName(cur.getString(colGSAuthorName))
                    .avgRating(cur.getDouble(colGSAvgRating))
                    .isShared(cur.getInt(colGSIsShared) != 0)
                    .isSynced(cur.getInt(colGSIsSynced) != 0)
                    .userRating(cur.getInt(colGSUserRating))
                    .userComment(cur.getString(colGSUserComment))
                    .build();
            AndroidDB.getTracks(mContext.getContentResolver(), pl);
        }
        closeCursor(cur);
        return pl;
    }

    /*
     * lib_entry specific methods
     */
    /**
     * @see #getEntries(Object)
     * @return
     */
    private Set<MusicItem> getMusicItems() {
        Cursor cur = getCursor(ConstDB.LIBE_TABLE_NAME,
                new String[] {
                        ConstDB.LIBE_C_LOCAL_ID, ConstDB.LIBE_C_ARTIST, ConstDB.LIBE_C_TITLE
                });
        Set<MusicItem> set = new HashSet<MusicItem>();
        if (cur != null && cur.moveToFirst()) {
            int colId = cur.getColumnIndex(ConstDB.LIBE_C_LOCAL_ID);
            int colArtist = cur.getColumnIndex(ConstDB.LIBE_C_ARTIST);
            int colTitle = cur.getColumnIndex(ConstDB.LIBE_C_TITLE);
            do {
                set.add(new MusicItem(cur.getInt(colId),
                        cur.getString(colArtist), cur.getString(colTitle)));
            } while (cur.moveToNext());
        }
        closeCursor(cur);
        return set;
    }

    // /**
    // * @see #isEmpty(Class)
    // * @return
    // */
    // private boolean libeIsEmpty() {
    // Cursor cur = getCursor(ConstDB.LIBE_TABLE_NAME,
    // new String[] {
    // ConstDB.LIBE_C_LOCAL_ID, ConstDB.LIBE_C_ARTIST, ConstDB.LIBE_C_TITLE
    // });
    // boolean isEmpty = !cur.moveToFirst();
    // closeCursor(cur);
    // return isEmpty;
    // }

    public void insert(MusicItem item) {
        ContentValues values = new ContentValues();
        values.put(ConstDB.LIBE_C_LOCAL_ID, item.localId);
        values.put(ConstDB.LIBE_C_ARTIST, item.artist);
        values.put(ConstDB.LIBE_C_TITLE, item.title);

        openW();
        mDB.insert(ConstDB.LIBE_TABLE_NAME, null, values);
        close();
    }

    public int delete(MusicItem item) {
        openW();
        int res = mDB.delete(ConstDB.LIBE_TABLE_NAME, LIBE_WHERE_ALL,
                new String[] {
                        String.valueOf(item.localId), item.artist, item.title
                });
        close();
        return res;
    }

    public boolean exists(MusicItem item) {
        open();
        Cursor cur = mDB.query(ConstDB.LIBE_TABLE_NAME,
                new String[] {
                    ConstDB.LIBE_C_LOCAL_ID
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
        mDB.delete(ConstDB.LIBE_TABLE_NAME, null, null);
        close();
    }

    public LinkedHashMap<String, Integer> getLibEntries() {
        Cursor cursor = getCursor(ConstDB.LIBE_TABLE_NAME, new String[] {
                ConstDB.C_ID, ConstDB.LIBE_C_TITLE, ConstDB.LIBE_C_ARTIST
        });
        LinkedHashMap<String, Integer> tags = null;
        // List<CharSequence> tags = null;
        if (cursor != null && cursor.moveToFirst()) {
            tags = new LinkedHashMap<String, Integer>();
            int colId = cursor.getColumnIndex(ConstDB.C_ID);
            int colTitle = cursor.getColumnIndex(ConstDB.LIBE_C_TITLE);
            int colArtist = cursor.getColumnIndex(ConstDB.LIBE_C_ARTIST);
            do {
                tags.put(cursor.getString(colTitle) + " - " + cursor.getString(colArtist),
                        cursor.getInt(colId));
            } while (cursor.moveToNext());
        }
        closeCursor(cursor);
        return tags;
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
        Cursor cursor = getCursor(ConstDB.TAG_TABLE_NAME, new String[] {
                ConstDB.C_ID, ConstDB.TAG_C_NAME, ConstDB.C_IS_CHECKED
        });
        return cursor;
    }

    public void tagSetChecked(int tagId, boolean isChecked) {
        openW();
        ContentValues values = new ContentValues();
        if (isChecked) {
            values.put(ConstDB.C_IS_CHECKED, ConstDB.TRUE);
        } else {
            values.put(ConstDB.C_IS_CHECKED, ConstDB.FALSE);
        }
        mDB.update(ConstDB.TAG_TABLE_NAME, values, "_id = ? ", new String[] {
                String.valueOf(tagId)
        });
        close();
    }

    public LinkedHashMap<String, Integer> getTags() {
        Cursor cursor = getCursor(ConstDB.TAG_TABLE_NAME, new String[] {
                ConstDB.C_ID, ConstDB.TAG_C_NAME
        });
        LinkedHashMap<String, Integer> tags = null;
        if (cursor != null && cursor.moveToFirst()) {
            tags = new LinkedHashMap<String, Integer>();
            int colId = cursor.getColumnIndex(ConstDB.C_ID);
            int colName = cursor.getColumnIndex(ConstDB.TAG_C_NAME);
            do {
                tags.put(cursor.getString(colName), cursor.getInt(colId));
            } while (cursor.moveToNext());
        }
        closeCursor(cursor);
        return tags;
    }

    private Set<TagItem> getTagItems() {
        Cursor cur = getCursor(ConstDB.TAG_TABLE_NAME,
                new String[] {
                        ConstDB.C_ID, ConstDB.TAG_C_NAME
                });
        Set<TagItem> set = new HashSet<TagItem>();
        if (cur != null && cur.moveToFirst()) {
            int colId = cur.getColumnIndex(ConstDB.C_ID);
            int colName = cur.getColumnIndex(ConstDB.TAG_C_NAME);
            int colIsChecked = cur.getColumnIndex(ConstDB.C_IS_CHECKED);
            int colRemoteId = cur.getColumnIndex(ConstDB.TAG_C_REMOTE_ID);
            do {
                set.add(new TagItem(cur.getInt(colId),
                        cur.getString(colName), cur.getInt(colIsChecked),
                        cur.getLong(colRemoteId)));
            } while (cur.moveToNext());
        }
        closeCursor(cur);
        return set;
    }

    // /**
    // * @see #isEmpty(Class)
    // * @return
    // */
    // private boolean tagIsEmpty() {
    // Cursor cur = getCursor(ConstDB.TAG_TABLE_NAME,
    // new String[] {
    // ConstDB.C_ID, ConstDB.TAG_C_NAME
    // });
    // boolean isEmpty = !cur.moveToFirst();
    // closeCursor(cur);
    // return isEmpty;
    // }

    private void tagTruncate() {
        openW();
        mDB.delete(ConstDB.TAG_TABLE_NAME, null, null);
        close();
    }

    private void plylTruncate() {
        openW();
        mDB.delete(ConstDB.PLAYLISTS_TABLE_NAME, null, null);
        close();
    }

    /**
     * Inserts the item only if it still does not exists.
     * 
     * @param item
     */
    public void insert(TagItem item) {
        ContentValues values = new ContentValues();
        // values.put(Const.TAGS_C_ID, item.localId);
        values.put(ConstDB.TAG_C_NAME, item.name);
        // values.put(Const.TAG_C_REMOTE_ID, item.remoteId);

        if (!exists(item)) {
            openW();
            mDB.insert(ConstDB.TAG_TABLE_NAME, null, values);
            close();
        }
    }

    public int delete(TagItem item) {
        openW();
        int res = mDB.delete(ConstDB.TAG_TABLE_NAME, TAGS_WHERE_ALL,
                new String[] {
                        String.valueOf(item.localId), item.name
                });
        close();
        return res;
    }

    /**
     * Doesn't work, why?
     * 
     * @param item
     * @return
     */
    public boolean exists(TagItem item) {
        open();
        Cursor cur = mDB.query(ConstDB.TAG_TABLE_NAME,
                new String[] {
                    ConstDB.TAG_C_NAME
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
        return ConstDB.C_ID;
    }

    public String tagGetColumnLabelIsChecked() {
        return ConstDB.C_IS_CHECKED;
    }

    public String tagGetColumnLabelName() {
        return ConstDB.TAG_C_NAME;
    }

    public void tagEmpty() {
        openW();
        mDB.delete(ConstDB.TAG_TABLE_NAME, null, null);
        close();
    }

    /*
     * COMMON
     */

    /**
     * @param type
     * @param items
     * @param checked
     */
    public void setChecked(SeedType type, LinkedHashMap<String, Integer> items,
            boolean[] checked) {
        openW();
        String table = null;
        switch (type) {
            case TAGS:
                table = ConstDB.TAG_TABLE_NAME;
                break;
            case TRACKS:
                table = ConstDB.LIBE_TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException();
        }
        ContentValues values = new ContentValues();
        Iterator<Entry<String, Integer>> it = items.entrySet().iterator();
        int index = 0;
        while (it.hasNext()) {
            if (checked[index]) {
                values.put(ConstDB.C_IS_CHECKED, ConstDB.TRUE);
            } else {
                values.put(ConstDB.C_IS_CHECKED, ConstDB.FALSE);
            }
            Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>) it.next();
            int tagId = pair.getValue();
            mDB.update(table, values, ConstDB.C_ID + " = ? ", new String[] {
                    String.valueOf(tagId)
            });
            index++;
        }
        close();
    }

    public JSONObject getCheckedItems(SeedType key) {
        String table = null;
        String[] columns = null;
        switch (key) {
            case TAGS:
                table = ConstDB.TAG_TABLE_NAME;
                columns = new String[] {
                        ConstDB.TAG_C_NAME
                };
                break;
            case TRACKS:
                table = ConstDB.LIBE_TABLE_NAME;
                columns = new String[] {
                        ConstDB.LIBE_C_TITLE, ConstDB.LIBE_C_ARTIST
                };
                break;
            default:
                throw new IllegalArgumentException();
        }
        Cursor cur = getCursor(table, columns, ConstDB.C_IS_CHECKED + " = ? ", new String[] {
                String.valueOf(ConstDB.TRUE)
        });

        JSONObject json = null;
        if (cur != null && cur.moveToFirst()) {
            json = new JSONObject();
            switch (key) {
                case TAGS:
                    int colName = cur.getColumnIndex(ConstDB.TAG_C_NAME);
                    do {
                        try {
                            // Ugly trick to get around missing json.append in
                            // android API
                            if (json.has(key.getLabel())) {
                                json.accumulate(key.getLabel(), cur.getString(colName));
                            } else {
                                json.put(key.getLabel(),
                                        new JSONArray().put(cur.getString(colName)));
                            }
                        } catch (JSONException e) {
                            Log.i(TAG, e.getMessage());
                        }
                    } while (cur.moveToNext());
                    break;
                case TRACKS:
                    int colTitle = cur.getColumnIndex(ConstDB.LIBE_C_TITLE);
                    int colArtist = cur.getColumnIndex(ConstDB.LIBE_C_ARTIST);
                    JSONObject track = new JSONObject();
                    do {
                        try {
                            track.put("title", cur.getString(colTitle));
                            track.put("artist", cur.getString(colArtist));
                            // Ugly trick to get around missing json.append in
                            // android API
                            if (json.has(key.getLabel())) {
                                json.accumulate(key.getLabel(), track.toString());
                            } else {
                                json.put(key.getLabel(),
                                        new JSONArray().put(track.toString()));
                            }
                        } catch (JSONException e) {
                            Log.i(TAG, e.getMessage());
                        }
                    } while (cur.moveToNext());
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
        closeCursor(cur);
        resetIsChecked(table);
        return json;
    }

    /**
     * Adds the playlist to the android sqlite DB and to the GS in-app DB.<br />
     * The insertions in the databases are made in an atomic way. If a failure
     * occurs when trying to insert the playlist in either the Android or GS
     * in-app database, changes done until failure are rolled back as if nothing
     * happened.
     * 
     * @param pl
     * @return local id
     */
    public long insert(PlaylistItem pl) {
        openW();
        mDB.beginTransaction();
        try {
            // First store to android DB
            AndroidDB.insert(mContext.getContentResolver(), pl);

            // Then insert a record to the device-local GS database
            if (pl.getLocalId() >= 0) {
                ContentValues values = new ContentValues();
                values.put(ConstDB.PLYL_C_LOCAL_ID, pl.getLocalId());
                values.put(ConstDB.PLYL_C_GS_SIZE, pl.getSize());
                values.put(ConstDB.PLYL_C_CREATED_BY_GS, 1);
                values.put(ConstDB.PLYL_C_GS_ID, pl.getPLId());
                values.put(ConstDB.PLYL_C_GS_CREATION_TIME, pl.getCreationTime());
                values.put(ConstDB.PLYL_C_GS_UPDATE_TIME, pl.getLastUpdated());
                values.put(ConstDB.PLYL_C_GS_AUTHOR_ID, pl.getAuthorId());
                values.put(ConstDB.PLYL_C_GS_AUTHOR_NAME, pl.getAuthorName());
                values.put(ConstDB.PLYL_C_GS_AVG_RATING, pl.getAvgRating());
                values.put(ConstDB.PLYL_C_GS_IS_SHARED, pl.isIsShared());
                values.put(ConstDB.PLYL_C_GS_IS_SYNCED, pl.isIsSynced());

                long plId = mDB.insert(ConstDB.PLAYLISTS_TABLE_NAME, null, values);
                Log.i(TAG, "Added playlist to GS in-app DB with id=" + plId + " (localId="
                        + pl.getLocalId() + ")");
                if (plId < 0) {
                    throw new SQLiteException("Playlist " + pl.toString()
                            + " could not be inserted to " + ConstDB.PLAYLISTS_TABLE_NAME);
                }
                mDB.setTransactionSuccessful();
            }
        } catch (SQLiteException sqle) {
            // An error occured, roll back should happen
            sqle.printStackTrace();
        } finally {
            mDB.endTransaction();
            close();
        }
        return pl.getLocalId();
    }

    private int delete(PlaylistItem pl) {
        int res = 0;
        if (pl.getLocalId() >= 0) {
            openW();
            mDB.beginTransaction();

            try {
                res = mDB.delete(ConstDB.PLAYLISTS_TABLE_NAME, ConstDB.PLYL_C_LOCAL_ID + " = ?",
                        new String[] {
                            String.valueOf(pl.getLocalId())
                        });
                if (res < 1) {
                    throw new SQLiteException("Playlist with local_id " + pl.getLocalId()
                            + " could not be removed from "
                            + ConstDB.PLAYLISTS_TABLE_NAME);
                }
                res += AndroidDB.delete(mContext.getContentResolver(), pl);
                mDB.setTransactionSuccessful();
            } catch (SQLiteException sqle) {
                // An error occured, roll back should happen
                sqle.printStackTrace();
            } finally {
                mDB.endTransaction();
                close();
            }
        }
        return res;
    }
}
