
package ch.epfl.unison.data;

import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.epfl.unison.Const;
import ch.epfl.unison.Uutils;
import ch.epfl.unison.api.JsonStruct.Track;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Abstraction of a playlist. A Playlist object could be shared between the
 * single-user mode and groups.
 * 
 * @author marc
 */
public class PlaylistItem extends AbstractItem<PlaylistItem> {

    private static final String TAG = PlaylistItem.class.getName();

    /** How is the playlist played. */
    private enum Mode {
        Linear, Circular, LoopOnTrack, Shuffle
    }
    
    /**
     * 
     * @author marc
     *
     */
    private static final class JsonKey {
        static final String LOCAL_ID = "local_id";
        static final String ARTIST = "artist";
        static final String TITLE = "title";
        static final String PLAY_ORDER = "play_order";
        
        private JsonKey() { }
    }

    private long mLocalId = -1; // Android sqlite
    private int mSize = -1;
    private Date mLocalLastUpdated;
    private boolean mCreatedByGS; // maybe useless
    private long mGSPLId; // GS database id
    private String mTitle;
    private Date mGSCreated;
    private Date mGSLastUpdated;
    private Date mDateCreated; // unused for now
    private long mDateModified; // from android
    private int mAuthorId;
    private String mAuthorName;
    private long mUserId;
    private int mGSSize;
    private List<TrackItem> mTracks;
    private int mUserRating;
    private String mUserComment;
    private int mListeners;
    private double mAvgRating;
    private boolean mIsShared;
    private boolean mIsSynced;

    private HashMap<String, Object> mOptions;

    // private LinkedList<MusicItem> mPlaylist;
    private int mCurrent = 0;
    private Mode mMode = Mode.Linear;
    private LinkedList<Integer> mShuffled = new LinkedList<Integer>();

    private Random mRandom = new Random();

    public PlaylistItem() {
        mTracks = Collections.synchronizedList(new LinkedList<TrackItem>());
        mOptions = new HashMap<String, Object>();
    }

    public PlaylistItem(long localId, String name) {
        mLocalId = localId;
        mTitle = name;
    }

    /**
     * @author marc
     */
    public static class Builder {
        private long mLocalId; // Android sqlite
        private int mSize;
        private Date mLocalLastUpdated;
        private String mTitle;
        private boolean mCreatedByGS;
        private long mGSPLId; // GS database id
        private Date mGSCreated;
        private Date mGSUpdated;
        private long mDateModified;
        private int mAuthorId;
        private String mAuthorName; // Not yet available
        private long mUserId;
        private int mGSSize;
        private LinkedList<TrackItem> mTracks;
        private int mUserRating;
        private String mUserComment;
        private int mListeners;
        private double mAvgRating;
        private boolean mIsShared;
        private boolean mIsSynced;

        public Builder localId(long id) {
            this.mLocalId = id;
            return this;
        }

        public Builder locaUpdated(Date d) {
            this.mLocalLastUpdated = d;
            return this;
        }

        public Builder localUpdated(String s) {
            try {
                this.mLocalLastUpdated = Uutils.stringToDate(s);
            } catch (ParseException e) {
                this.mLocalLastUpdated = null;
                e.printStackTrace();
            }
            return this;
        }

        public Builder createdByGS(boolean b) {
            this.mCreatedByGS = b;
            return this;
        }

        public Builder plId(long id) {
            this.mGSPLId = id;
            return this;
        }

        public Builder avgRating(double avgRating) {
            this.mAvgRating = avgRating;
            return this;
        }

        public Builder title(String t) {
            this.mTitle = t;
            return this;
        }

        public Builder created(Date c) {
            this.mGSCreated = c;
            return this;
        }

        /**
         * @param c datetime in ISO format
         * @return
         */
        public Builder created(String c) {
            try {
                this.mGSCreated = Uutils.stringToDate(c);
            } catch (ParseException e) {
                this.mGSCreated = null;
                e.printStackTrace();
            }
            return this;
        }

        public Builder updated(Date c) {
            this.mGSUpdated = c;
            return this;
        }

        /**
         * @param u datetime in ISO format
         * @return
         */
        public Builder gsUpdated(String u) {
            try {
                this.mGSUpdated = Uutils.stringToDate(u);
            } catch (ParseException e) {
                this.mGSCreated = null;
                e.printStackTrace();
            }
            return this;
        }

//        public Builder modified(Date d) {
//            this.mDateModified = d;
//            return this;
//        }
        
        public Builder modified(long l) {
            this.mDateModified = l;
            return this;
        }

//        public Builder modified(String u) {
//            try {
//                this.mDateModified = Uutils.stringToDate(u);
//            } catch (ParseException e) {
//                this.mDateModified = null;
//                e.printStackTrace();
//            }
//            return this;
//        }

        public Builder authorId(int id) {
            this.mAuthorId = id;
            return this;
        }

        public Builder authorName(String name) {
            this.mAuthorName = name;
            return this;
        }

        public Builder userId(long uid) {
            this.mUserId = uid;
            return this;
        }

        public Builder size(int i) {
            this.mSize = i;
            return this;
        }

        public Builder tracks(LinkedList<TrackItem> ll) {
            this.mTracks = ll;
            this.mSize = mTracks.size();
            return this;
        }

        public Builder tracks(Track[] t) {
            LinkedList<TrackItem> ll = new LinkedList<TrackItem>();
            if (t != null) {
                for (int i = 0; i < t.length; i++) {
                    ll.add(new TrackItem(t[i].localId, t[i].artist, t[i].title,
                            t[i].playOrder));
                }
            }
            this.mTracks = ll;
            this.mSize = mTracks.size();
            return this;
        }
        
        /**
         * Tracks in JSONArray format.
         * 
         * @param json
         * @return the builder
         */
        public Builder tracks(String json) {
            LinkedList<TrackItem> ll = new LinkedList<TrackItem>();
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject t = (JSONObject) array.get(i);
                    ll.add(new TrackItem(
                            t.getLong(JsonKey.LOCAL_ID), 
                            t.getString(JsonKey.ARTIST), 
                            t.getString(JsonKey.TITLE),
                            t.getLong(JsonKey.PLAY_ORDER)));
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return this;
        }

        public Builder listeners(int i) {
            this.mListeners = i;
            return this;
        }

        public Builder isShared(boolean isShared) {
            this.mIsShared = isShared;
            return this;
        }

        public Builder isSynced(boolean isSynced) {
            this.mIsSynced = isSynced;
            return this;
        }

        public Builder userRating(int userRating) {
            this.mUserRating = userRating;
            return this;
        }

        public Builder userComment(String userComment) {
            this.mUserComment = userComment;
            return this;
        }

        public PlaylistItem build() {
            return new PlaylistItem(this);
        }
    }

    private PlaylistItem(Builder builder) {
        this.mLocalId = builder.mLocalId;
        this.mSize = builder.mSize;
        this.mLocalLastUpdated = builder.mLocalLastUpdated;
        this.mCreatedByGS = builder.mCreatedByGS;
        this.mGSPLId = builder.mGSPLId;
        this.mTitle = builder.mTitle;
        this.mGSCreated = builder.mGSCreated;
        this.mGSLastUpdated = builder.mGSUpdated;
        this.mDateModified = builder.mDateModified;
        this.mAuthorId = builder.mAuthorId;
        this.mAuthorName = builder.mAuthorName;
        this.mUserId = builder.mUserId;
        this.mGSSize = builder.mGSSize;
        this.mTracks = builder.mTracks;
        this.mUserRating = builder.mUserRating;
        this.mUserComment = builder.mUserComment;
        this.mListeners = builder.mListeners;
        this.mAvgRating = builder.mAvgRating;
        this.mIsShared = builder.mIsShared;
        this.mIsSynced = builder.mIsSynced;
    }

    /*
     * Player specific interaction
     */

    private boolean hasNext() {
        synchronized (mTracks) {
            if (mCurrent < mTracks.size() - 1) {
                return true;
            }
            return false;
        }
    }

    public TrackItem next() {
        synchronized (mTracks) {
            if (hasNext()) {
                mCurrent++;
                if (mMode == Mode.Shuffle) {
                    return mTracks.get(mShuffled.get(mCurrent));
                }
            } else if (mMode == Mode.Circular) {
                mCurrent = 0;
            } else {
                throw new IndexOutOfBoundsException("No more tracks to play");
            }
            return mTracks.get(mCurrent);
        }
    }

    public TrackItem current() {
        synchronized (mTracks) {
            if (mCurrent >= 0 && mTracks.size() > 0 && mCurrent < mTracks.size()) {
                return mTracks.get(mCurrent);
            } else {
                throw new IndexOutOfBoundsException();
            }
        }
    }

    private boolean hasPrevious() {
        if (mCurrent >= 1) {
            return true;
        }
        return false;
    }

    public TrackItem previous() {
        synchronized (mTracks) {
            if (hasPrevious()) {
                mCurrent--;
                if (mMode == Mode.Shuffle) {
                    return mTracks.get(mShuffled.get(mCurrent));
                }
            } else if (mMode == Mode.Circular) {
                mCurrent = mTracks.size() - 1;
            } else {
                throw new IndexOutOfBoundsException("No more tracks to play");
            }
            return mTracks.get(mCurrent);
        }
    }

    /*
     * Getters and setters
     */

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    /**
     * @return The arbitrary size if set, else the number of tracks, -1 if
     *         nothing set.
     */
    public int size() {
        if (mSize >= 0) {
            return this.mSize;
        } else if (mTracks != null) {
            return this.mTracks.size();
        } else {
            return -1;
        }
    }

    public void setTracks(LinkedList<TrackItem> tracks) {
        this.mTracks = tracks;
        this.mSize = mTracks.size();
    }

    public String getLastUpdated() {
        return mGSLastUpdated.toString();
    }

    private void setLastUpdated(Date lastUpdated) {
        this.mGSLastUpdated = lastUpdated;
    }

    private void setLastUpdated(String lastUpdated) {
        try {
            this.mGSLastUpdated = Uutils.stringToDate(lastUpdated);
        } catch (ParseException e) {
            this.mGSLastUpdated = null;
            e.printStackTrace();
        }
    }

    private void setCreated(String created) {
        try {
            this.mGSCreated = Uutils.stringToDate(created);
        } catch (ParseException e) {
            this.mGSCreated = null;
            e.printStackTrace();
        }
    }
    
    public void setDateModified(long timestamp) {
        this.mDateModified = timestamp;
    }

    public int getUserRating() {
        return mUserRating;
    }

    public void setUserRating(int userRating) {
        this.mUserRating = userRating;
    }

    public PlaylistItem setUserId(long uid) {
        this.mUserId = uid;
        return this;
    }

    public String getUserComment() {
        return mUserComment;
    }

    public void setUserComment(String userComment) {
        this.mUserComment = userComment;
    }

    public boolean isIsShared() {
        return mIsShared;
    }

    public void setLocalId(long localId) {
        this.mLocalId = localId;
    }

    public void setIsShared(boolean isShared) {
        this.mIsShared = isShared;
    }

    public boolean isIsSynced() {
        return mIsSynced;
    }

    public void setIsSynced(boolean isSynced) {
        this.mIsSynced = isSynced;
    }

    public long getLocalId() {
        return mLocalId;
    }

    /**
     * 
     * @return GroupStreamer playlist id
     */
    public long getPlaylistId() {
        return mGSPLId;
    }

    public String getCreationTime() {
        return mGSCreated.toString();
    }
    
    public long getDateModified() {
        return mDateModified;
    }

    public int getAuthorId() {
        return mAuthorId;
    }

    public String getAuthorName() {
        return mAuthorName;
    }

    public long getUserId() {
        return mUserId;
    }

    /**
     * @return the size as computed on the GS server. If you want the real size
     *         of the playlist on the device, use {@link #size()} instead.
     */
    public int getGSSize() {
        return mGSSize;
    }
    
    public int getSize() {
        return mTracks.size();
    }

    public List<TrackItem> getTracks() {
        return mTracks;
    }
    
    public String getTracksJson() {
        JSONArray json = new JSONArray();
        JSONObject jsonTrack;
        for (TrackItem track : mTracks) {
            jsonTrack = new JSONObject();
            try {
                jsonTrack.put(JsonKey.LOCAL_ID, track.localId);
                jsonTrack.put(JsonKey.ARTIST, track.artist);
                jsonTrack.put(JsonKey.TITLE, track.title);
                jsonTrack.put(JsonKey.PLAY_ORDER, track.playOrder);
                json.put(jsonTrack);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return json.toString();
    }

    public int getListeners() {
        return mListeners;
    }

    public double getAvgRating() {
        return mAvgRating;
    }

    public HashMap<String, Object> getOptions() {
        return mOptions;
    }

    public List<TrackItem> getPlaylist() {
        return mTracks;
    }

    public void setMode(Mode mode) {
        switch (mode) {
            case Shuffle:
                mShuffled.clear();
                LinkedList<Integer> temp = new LinkedList<Integer>();
                Iterator<TrackItem> it = mTracks.iterator();
                while (it.hasNext()) {
                    temp.add((int) it.next().playOrder);
                }
                while (mShuffled.size() < mTracks.size()) {
                    mShuffled.add(temp.remove(mRandom.nextInt(temp.size())));
                }
                break;
            default:
                mMode = mode;
                break;
        }
    }

    public HashMap<String, String> toHashMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Const.Strings.TITLE, mTitle);
        map.put(Const.Strings.SIZE, String.valueOf(mSize));
        return map;
    }

    @Override
    public int compareTo(PlaylistItem another) {
        // TODO Auto-generated method stub
        return 0;
    }

}
