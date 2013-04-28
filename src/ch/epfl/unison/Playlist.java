
package ch.epfl.unison;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.Log;

import com.google.gson.JsonNull;

import ch.epfl.unison.Const.SeedType;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.JsonStruct.Track;
import ch.epfl.unison.data.MusicItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Abstraction of a playlist. A Playlist object could be shared between the
 * single-user mode and groups.
 * 
 * @author marc
 */
public class Playlist {

    private Long mLocalId; // Android sqlite
    private Long mPlId; // GS database id
    private String mTitle;
    private Calendar mCreated;
    private Calendar mLastUpdated;
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
    
    // private HashMap<SeedType, LinkedList<Long>> mRaw;
    private ArrayList<Integer> mRawTagsId;
    // TODO mRawTracks
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
        private Long mLocalId; // Android sqlite
        private Long mId; // GS database id
        private String mTitle;
        private Calendar mCreated;
        private Calendar mLastUpdated;
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
        
        public Builder localId(Long id) {
            this.mLocalId = id;
            return this;
        }
        
        public Builder id(Long id) {
            this.mId = id;
            return this;
        }
        
        public Builder title(String t) {
            this.mTitle = t;
            return this;
        }
        
        public Builder created(Calendar c) {
            this.mCreated = c;
            return this;
        }
        
        public Builder updated(Calendar c) {
            this.mLastUpdated = c;
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
            for (int i = 0; i < t.length; i++) {
                ll.add(new MusicItem(t[i].localId, t[i].artist, t[i].title));
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
    
    private Playlist(Builder builder) {
        this.mPlId = builder.mId;
        this.mTitle = builder.mTitle;
        this.mCreated = builder.mCreated;
        this.mLastUpdated = builder.mLastUpdated;
        this.mAuthorId = builder.mAuthorId;
        this.mSize = builder.mSize;
        this.mTracks = builder.mTracks;
        this.mListeners = builder.mListeners;
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
    @SuppressLint("NewApi")
    public JSONObject export(Resources res) {

        if (mRawTagsId.isEmpty()) {
            return null;
        }

        JSONObject json = new JSONObject();

        // Tags
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < mRawTagsId.size(); i++) {
//            jsonArray.put(tags.getString(i));
            jsonArray.put(JSONObject.NULL);
        }

        // Tracks
        // TODO

        try {
            json.put(SeedType.TAGS.getLabel(), jsonArray);
            // json.put(SeedType.TRACKS.getLabel(), tracksInString);
            if (!mOptions.isEmpty()) {
                // TODO
                String foo = null;
            }
            return json;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }


    public String getTitle() {
        return mTitle;
    }


    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }


    public Calendar getLastUpdated() {
        return mLastUpdated;
    }


    public void setLastUpdated(Calendar mLastUpdated) {
        this.mLastUpdated = mLastUpdated;
    }


    public int getUserRating() {
        return mUserRating;
    }


    public void setUserRating(int mUserRating) {
        this.mUserRating = mUserRating;
    }


    public String getUserComment() {
        return mUserComment;
    }


    public void setUserComment(String mUserComment) {
        this.mUserComment = mUserComment;
    }


    public boolean isIsShared() {
        return mIsShared;
    }


    public void setIsShared(boolean mIsShared) {
        this.mIsShared = mIsShared;
    }


    public boolean isIsSynced() {
        return mIsSynced;
    }


    public void setIsSynced(boolean mIsSynced) {
        this.mIsSynced = mIsSynced;
    }


    public void setRawTagsId(ArrayList<Integer> mRawTagsId) {
        this.mRawTagsId = mRawTagsId;
    }


    public Long getLocalId() {
        return mLocalId;
    }


    public Long getPlId() {
        return mPlId;
    }


    public Calendar getCreated() {
        return mCreated;
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
