
package ch.epfl.unison.data;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import ch.epfl.unison.api.JsonStruct.Track;

/**
 * Abstraction of a playlist. A Playlist object could be shared between the
 * single-user mode and groups.
 * 
 * @author marc
 */
public class Playlist {
    
    private static final String TAG = Playlist.class.getName();

    private int mLocalId = 0; // Android sqlite
    private int mGSPLId; // GS database id
    private String mTitle;
    private Date mCreated;
    private Date mLastUpdated;
    private int mAuthorId;
    private String mAuthorName;
    private int mSize;
    private LinkedList<MusicItem> mTracks; 
    private int mUserRating;
    private String mUserComment;
    private int mListeners;
    private double mAvgRating;
    private boolean mIsShared;
    private boolean mIsSynced;
    private static DateFormat smDF = DateFormat.getInstance();
    
    // private HashMap<SeedType, LinkedList<Long>> mRaw;
    private ArrayList<Integer> mRawTagsId;
    // TODO mRawTracks
//    private UnisonDB mDB;
    private HashMap<String, Object> mOptions;
    
    private LinkedList<MusicItem> mPlaylist;   

    public Playlist() {
        mRawTagsId = new ArrayList<Integer>();
        mTracks = new LinkedList<MusicItem>();
        mOptions = new HashMap<String, Object>();
    }
    
    /**
     * 
     * @author marc
     *
     * TODO complete
     */
    public static class Builder {
        private int mLocalId; // Android sqlite
        private int mGSPLId; // GS database id
        private String mTitle;
        private Date mCreated;
        private Date mLastUpdated;
        private int mAuthorId;
        //private String mAuthorName; // Not yet available
        private int mSize;
        private LinkedList<MusicItem> mTracks; 
        private int mUserRating;
        private String mUserComment;
        private int mListeners;
        private double mAvgRating;
        private boolean mIsShared;
        private boolean mIsSynced;
        
        public Builder localId(int id) {
            this.mLocalId = id;
            return this;
        }
        
        public Builder plId(int id) {
            this.mGSPLId = id;
            return this;
        }
        
        public Builder title(String t) {
            this.mTitle = t;
            return this;
        }
        
        public Builder created(Date c) {
            this.mCreated = c;
            return this;
        }
        
        /**
         * 
         * @param c datetime in ISO format
         * @return
         */
        public Builder created(String c) {
            try {
                this.mCreated = smDF.parse(c);
            } catch (ParseException e) {
                this.mCreated = null;
                e.printStackTrace();
            }
            return this;
        }
        
        
        public Builder updated(Date c) {
            this.mLastUpdated = c;
            return this;
        }
        
        /**
         * 
         * @param u datetime in ISO format
         * @return
         */
        public Builder updated(String u) {
            try {
                this.mCreated = smDF.parse(u);
            } catch (ParseException e) {
                this.mCreated = null;
                e.printStackTrace();
            }
            return this;
        }
        
        public Builder authorId(int id) {
            this.mAuthorId = id;
            return this;
        }
        
        public Builder size(int i) {
            this.mSize = i;
            return this;
        }
        
        public Builder tracks(LinkedList<MusicItem> ll) {
            this.mTracks = ll;
            return this;
        }
        
        public Builder tracks(Track[] t) {
            LinkedList<MusicItem> ll = new LinkedList<MusicItem>();
            if (t != null) {
                for (int i = 0; i < t.length; i++) {
                    ll.add(new MusicItem(t[i].localId, t[i].artist, t[i].title));
                }
            }
            this.mTracks = ll;
            return this;
        }
        
        public Builder listeners(int i) {
            this.mListeners = i;
            return this;
        }

        public Playlist build() {
            return new Playlist(this);
        }
    }
    
    /*
     * Minimalistic builder, should be upgraded to include all fields
     */
    private Playlist(Builder builder) {
        this.mLocalId = builder.mLocalId;
        this.mGSPLId = builder.mGSPLId;
        this.mTitle = builder.mTitle;
        this.mCreated = builder.mCreated;
        this.mLastUpdated = builder.mLastUpdated;
        this.mAuthorId = builder.mAuthorId;
        this.mSize = builder.mSize;
        this.mTracks = builder.mTracks;
        this.mUserRating = builder.mUserRating;
        this.mUserComment = builder.mUserComment;
        this.mListeners = builder.mListeners;
        this.mAvgRating = builder.mAvgRating;
        this.mIsShared = builder.mIsShared;
        this.mIsSynced = builder.mIsSynced;
    }
    

    public void addRawTags(ArrayList<Integer> seeds) {
//        Log.i("Playlist", "to be added to rawlist: " + seeds + "\n");
        mRawTagsId.addAll(seeds);
//        Log.i("Playlist", "rawlist: " + mRawTags.toString() + "\n");
    }

    /**
     * Helper to convert a TypedArray to a String in JSONObject format. The
     * TypedArray values are treated as Strings. Only the values with index from
     * indexes are selected.
     * 
     * @param key
     * @param values
     * @param indexes
     * @return null in case of failure
     */
//    @SuppressLint("NewApi")
//    public JSONObject export(Resources res) {
//
//        if (mRawTagsId.isEmpty()) {
//            return null;
//        }
//
//        JSONObject json = new JSONObject();
//
//        // Tags
//        JSONArray jsonArray = new JSONArray();
//        for (int i = 0; i < mRawTagsId.size(); i++) {
////            jsonArray.put(tags.getString(i));
//            jsonArray.put(JSONObject.NULL);
//        }
//
//        // Tracks
//        // TODO
//
//        try {
//            json.put(SeedType.TAGS.getLabel(), jsonArray);
//            // json.put(SeedType.TRACKS.getLabel(), tracksInString);
//            if (!mOptions.isEmpty()) {
//                // TODO convert mOptions in json format
//                Log.i(TAG, "There are options");
//            }
//            return json;
//        } catch (JSONException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return null;
//    }


    public String getTitle() {
        return mTitle;
    }


    public void setTitle(String title) {
        this.mTitle = title;
    }


    public String getLastUpdated() {
        return mLastUpdated.toString();
    }


    public void setLastUpdated(Date lastUpdated) {
        this.mLastUpdated = lastUpdated;
    }
    
    public void setLastUpdated(String lastUpdated) {
        try {
            this.mLastUpdated = smDF.parse(lastUpdated);
        } catch (ParseException e) {
            this.mLastUpdated = null;
            e.printStackTrace();
        }
    }
    
    public void setCreated(String created) {
        try {
            this.mCreated = smDF.parse(created);
        } catch (ParseException e) {
            this.mCreated = null;
            e.printStackTrace();
        }
    }


    public int getUserRating() {
        return mUserRating;
    }


    public void setUserRating(int userRating) {
        this.mUserRating = userRating;
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

    public void setLocalId(int localId) {
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


    public void setRawTagsId(ArrayList<Integer> rawTagsId) {
        this.mRawTagsId = rawTagsId;
    }


    public int getLocalId() {
        return mLocalId;
    }


    public int getPLId() {
        return mGSPLId;
    }


    public String getCreationTime() {
        return mCreated.toString();
    }


    public int getAuthorId() {
        return mAuthorId;
    }


    public String getAuthorName() {
        return mAuthorName;
    }


    public int getSize() {
        return mSize;
    }


    public LinkedList<MusicItem> getTracks() {
        return mTracks;
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


    public LinkedList<MusicItem> getPlaylist() {
        return mPlaylist;
    }

}
