
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
 * about the one that GroupStreamer owns, i.e. on the phone.
 * 
 * @author marc
 */
public final class UnisonDB {

    private static final String TAG = "ch.epfl.unison.UnisonDB";

    private SQLiteDatabase mDB;
    private final Context mContext;
    private final UnisonDBHelper mDbHelper;

    private Track mTrackHandler;
    private Playlist mPlaylistHandler;
    private Tag mTagHandler;

    public UnisonDB(Context c) {
        mContext = c;
        // Log.e(TAG + "UnisonDB", "REMOVE THE DB DELETION ON PROD APP");
        // mContext.deleteDatabase(ConstDB.DATABASE_NAME); //FIXME remove for
        // production app!
        mDbHelper = new UnisonDBHelper(mContext, ConstDB.DATABASE_NAME, null,
                ConstDB.DATABASE_VERSION);
        mTrackHandler = new Track();
        mPlaylistHandler = new Playlist();
        mTagHandler = new Tag();
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

    // private Cursor getCursorW(String table, String[] columns) {
    // openW();
    // return mDB.query(table, columns, null, null, null, null, null);
    // }

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

    public boolean exists(String table, String selection, String[] selectionArgs) {
        open();
        Cursor cur = mDB.query(table,
                new String[] {
                    ConstDB.LIBE_C_LOCAL_ID
                },
                selection,
                selectionArgs,
                null, null, null, "1"); // LIMIT 1
        boolean exists = cur.moveToFirst();
        closeCursor(cur);
        return exists;
    }

    private boolean isEmpty(String table) {
        Cursor cur = getCursor(table,
                new String[] {
                    ConstDB.C_ID
                });
        if (cur != null) {
            boolean isEmpty = !cur.moveToFirst();
            closeCursor(cur);
            return isEmpty;
        } else {
            throw new SQLiteException("Table " + table + " does not exist.");
        }
    }

    /**
     * All the inner classes have to implement these basic functionalities.
     * 
     * @author marc
     */
    private interface ITableHandler<T extends AbstractItem> {
        boolean isEmpty();

        boolean exists(T item);

        long insert(T item);

        T getItem(long index);

        Set<T> getItems();

        int delete(T item);

        void truncate();
    }

    /**
     * Music table handler.
     * 
     * @author marc
     */
    public final class Track implements ITableHandler<TrackItem> {

        private final String mTable = ConstDB.LIBE_TABLE_NAME; // Just an alias
        private static final String LIBE_WHERE_ALL = ConstDB.LIBE_C_LOCAL_ID + " = ? AND "
                + ConstDB.LIBE_C_ARTIST + " = ? AND " + ConstDB.LIBE_C_TITLE + " = ?";

        Track() {
        }

        @Override
        public boolean isEmpty() {
            return UnisonDB.this.isEmpty(mTable);
        }

        @Override
        public boolean exists(TrackItem item) {
            return UnisonDB.this.exists(mTable, LIBE_WHERE_ALL, new String[] {
                    String.valueOf(item.localId), item.artist, item.title
            });
        }

        @Override
        public long insert(TrackItem item) {
            ContentValues values = new ContentValues();
            values.put(ConstDB.LIBE_C_LOCAL_ID, item.localId);
            values.put(ConstDB.LIBE_C_ARTIST, item.artist);
            values.put(ConstDB.LIBE_C_TITLE, item.title);

            openW();
            long res = mDB.insert(ConstDB.LIBE_TABLE_NAME, null, values);
            close();
            return res;
        }

        @Override
        public TrackItem getItem(long index) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<TrackItem> getItems() {
            Cursor cur = getCursor(mTable,
                    new String[] {
                            ConstDB.LIBE_C_LOCAL_ID, ConstDB.LIBE_C_ARTIST, ConstDB.LIBE_C_TITLE
                    });
            Set<TrackItem> set = new HashSet<TrackItem>();
            if (cur != null && cur.moveToFirst()) {
                int colId = cur.getColumnIndex(ConstDB.LIBE_C_LOCAL_ID);
                int colArtist = cur.getColumnIndex(ConstDB.LIBE_C_ARTIST);
                int colTitle = cur.getColumnIndex(ConstDB.LIBE_C_TITLE);
                do {
                    set.add(new TrackItem(cur.getInt(colId),
                            cur.getString(colArtist), cur.getString(colTitle)));
                } while (cur.moveToNext());
            }
            closeCursor(cur);
            return set;
        }

        @Override
        public int delete(TrackItem item) {
            openW();
            int res = mDB.delete(mTable, LIBE_WHERE_ALL,
                    new String[] {
                            String.valueOf(item.localId), item.artist, item.title
                    });
            close();
            return res;
        }

        @Override
        public void truncate() {
            openW();
            mDB.delete(mTable, null, null);
            close();
        }

        public LinkedHashMap<String, Integer> getLibEntries() {
            Cursor cursor = getCursor(mTable, new String[] {
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

    }

    /**
     * Tag table handler.
     * 
     * @author marc
     */
    public final class Tag implements ITableHandler<TagItem> {

        private final String mTable = ConstDB.TAG_TABLE_NAME; // Just an alias
        private static final String TAGS_WHERE_ALL = ConstDB.C_ID + " = ? AND "
                + ConstDB.TAG_C_NAME + " = ? AND "
                + ConstDB.C_IS_CHECKED + " = ?";
        private static final String TAG_WHERE_NAME = ConstDB.TAG_C_NAME + " LIKE ? ";

        Tag() {
        }

        @Override
        public boolean isEmpty() {
            return UnisonDB.this.isEmpty(mTable);
        }

        @Override
        public boolean exists(TagItem item) {
            return UnisonDB.this.exists(mTable, TAG_WHERE_NAME, new String[] {
                    item.name
            });
        }

        /**
         * Inserts the item only if it still does not exists.
         * 
         * @param item
         */
        @Override
        public long insert(TagItem item) {
            ContentValues values = new ContentValues();
            // values.put(Const.TAGS_C_ID, item.localId);
            values.put(ConstDB.TAG_C_NAME, item.name);
            // values.put(Const.TAG_C_REMOTE_ID, item.remoteId);

            long rowId = -1;
            if (!exists(item)) {
                openW();
                rowId = mDB.insert(mTable, null, values);
                close();
            }
            return rowId;
        }

        @Override
        public TagItem getItem(long index) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<TagItem> getItems() {
            Cursor cur = getCursor(mTable,
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

        @Override
        public int delete(TagItem item) {
            openW();
            int res = mDB.delete(mTable, TAGS_WHERE_ALL,
                    new String[] {
                            String.valueOf(item.localId), item.name
                    });
            close();
            return res;
        }

        @Override
        public void truncate() {
            openW();
            mDB.delete(mTable, null, null);
            close();
        }

        // public void setChecked(int tagId, boolean isChecked) {
        // openW();
        // ContentValues values = new ContentValues();
        // if (isChecked) {
        // values.put(ConstDB.C_IS_CHECKED, ConstDB.TRUE);
        // } else {
        // values.put(ConstDB.C_IS_CHECKED, ConstDB.FALSE);
        // }
        // mDB.update(mTable, values, "_id = ? ", new String[] {
        // String.valueOf(tagId)
        // });
        // close();
        // }

        public LinkedHashMap<String, Integer> getTags() {
            Cursor cursor = getCursor(mTable, new String[] {
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
                                // Ugly trick to get around missing json.append
                                // in
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
                                // Ugly trick to get around missing json.append
                                // in
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

    }

    /**
     * Playlist table handler.
     * 
     * @author marc
     */
    public final class Playlist implements ITableHandler<PlaylistItem> {

        private final String mTable = ConstDB.PLAYLISTS_TABLE_NAME; // Just an
                                                                    // alias

        Playlist() {
        }

        @Override
        public boolean isEmpty() {
            return UnisonDB.this.isEmpty(mTable);
        }

        @Override
        public boolean exists(PlaylistItem item) {
            // TODO Auto-generated method stub
            return false;
        }

        /**
         * Adds the playlist to the android sqlite DB and to the GS in-app DB.<br />
         * The insertions in the databases are made in an atomic way. If a
         * failure occurs when trying to insert the playlist in either the
         * Android or GS in-app database, changes done until failure are rolled
         * back as if nothing happened.
         * 
         * @param pl
         * @return local id
         */
        @Override
        public long insert(PlaylistItem item) {
            openW();
            mDB.beginTransaction();
            try {
                // First store to android DB
                AndroidDB.insert(mContext.getContentResolver(), item);
                // Then insert a record to the device-local GS database
                if (item.getLocalId() >= 0) {
                    ContentValues values = new ContentValues();
                    values.put(ConstDB.PLYL_C_LOCAL_ID, item.getLocalId());
                    values.put(ConstDB.PLYL_C_TRACKS, item.getTracksJson());
                    values.put(ConstDB.PLYL_C_LOCAL_UPDATE_TIME, item.getDateModified());
                    values.put(ConstDB.PLYL_C_GS_USER_ID, item.getUserId());
                    values.put(ConstDB.PLYL_C_GS_SIZE, item.getSize());
                    values.put(ConstDB.PLYL_C_CREATED_BY_GS, 1);
                    values.put(ConstDB.PLYL_C_GS_ID, item.getPlaylistId());
                    values.put(ConstDB.PLYL_C_GS_CREATION_TIME, item.getCreationTime());
                    values.put(ConstDB.PLYL_C_GS_UPDATE_TIME, item.getLastUpdated());
                    values.put(ConstDB.PLYL_C_GS_AUTHOR_ID, item.getAuthorId());
                    values.put(ConstDB.PLYL_C_GS_AUTHOR_NAME, item.getAuthorName());
                    values.put(ConstDB.PLYL_C_GS_AVG_RATING, item.getAvgRating());
                    values.put(ConstDB.PLYL_C_GS_IS_SHARED, item.isIsShared());
                    values.put(ConstDB.PLYL_C_GS_IS_SYNCED, item.isIsSynced());

                    long plId = mDB.insert(mTable, null, values);
                    Log.i(TAG, "Added playlist to GS in-app DB with id=" + plId + " (localId="
                            + item.getLocalId() + ")");
                    if (plId < 0) {
                        throw new SQLiteException("Playlist " + item.toString()
                                + " could not be inserted to " + mTable);
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
            return item.getLocalId();
        }

        @Override
        public PlaylistItem getItem(long index) {
            Cursor cur = getCursor(mTable,
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
                        String.valueOf(index)
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

        @Override
        public Set<PlaylistItem> getItems() {
            Cursor cur = getCursor(mTable,
                    new String[] {
                            ConstDB.PLYL_C_LOCAL_ID,
                            ConstDB.PLYL_C_TRACKS,
                            ConstDB.PLYL_C_LOCAL_UPDATE_TIME,
                            ConstDB.PLYL_C_CREATED_BY_GS,
                            ConstDB.PLYL_C_GS_SIZE,
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
                int colTracks = cur.getColumnIndex(ConstDB.PLYL_C_TRACKS);
                int colDateModified = cur.getColumnIndex(ConstDB.PLYL_C_LOCAL_UPDATE_TIME);
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
                    set.add(new PlaylistItem.Builder()
                            .localId(cur.getInt(colLocalId))
                            .tracks(cur.getString(colTracks))
                            .modified(cur.getLong(colDateModified))
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

        @Override
        public int delete(PlaylistItem item) {
            int res = 0;
            if (item.getLocalId() >= 0) {
                openW();
                mDB.beginTransaction();

                try {
                    res = mDB.delete(mTable, ConstDB.PLYL_C_LOCAL_ID + " = ?",
                            new String[] {
                                String.valueOf(item.getLocalId())
                            });
                    if (res < 1) {
                        throw new SQLiteException("Playlist with local_id " + item.getLocalId()
                                + " could not be removed from "
                                + mTable);
                    }
                    res += AndroidDB.delete(mContext.getContentResolver(), item);
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

        @Override
        public void truncate() {
            openW();
            mDB.delete(mTable, null, null);
            close();
        }

        public boolean isMadeWithGS(long id) {
            Cursor cur = getCursor(mTable,
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

        public boolean isMadeWithGS(long plid, long uid) {
            String selection = ConstDB.PLYL_C_LOCAL_ID + " = ? "
                    + "AND " + ConstDB.PLYL_C_CREATED_BY_GS + " = ? "
                    + "AND " + ConstDB.PLYL_C_GS_USER_ID + " = ? ";
            Cursor cur = getCursor(mTable,
                    new String[] {
                            ConstDB.PLYL_C_LOCAL_ID,
                            ConstDB.PLYL_C_CREATED_BY_GS,
                            ConstDB.PLYL_C_GS_USER_ID
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
        
        /**
         * 
         * @param uid
         * @return an empty Set if no match found, a non-empty Set else
         */
        public Set<PlaylistItem> getItems(long uid) {
            String selection = ConstDB.PLYL_C_GS_USER_ID + " = ? ";
            String[] selectionArgs = new String[] {String.valueOf(uid)};
            Cursor cur = getCursor(mTable,
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
                    },
                    selection,
                    selectionArgs);
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

    }

    /*
     * GETTERS
     */

    public Track getTrackHandler() {
        return mTrackHandler;
    }

    public Playlist getPlaylistHandler() {
        return mPlaylistHandler;
    }

    public Tag getTagHandler() {
        return mTagHandler;
    }

}
