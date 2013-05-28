
package ch.epfl.unison.data;

import ch.epfl.unison.Uutils;
import ch.epfl.unison.api.JsonStruct.Track;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Abstraction of a playlist. A Playlist object could be shared between the
 * single-user mode and groups.
 * 
 * @author marc
 */
public class PlaylistItem extends AbstractItem {
    
    private static final String TAG = PlaylistItem.class.getName();

    private long mLocalId = 0; // Android sqlite
    private Date mLocalLastUpdated;
    private boolean mCreatedByGS; // maybe useless
    private long mGSPLId; // GS database id
    private String mTitle;
    private Date mGSCreated;
    private Date mGSLastUpdated;
    private Date mDateCreated; // unused for now
    private Date mDateModified; // from android
    private int mAuthorId;
    private String mAuthorName;
    private int mGSSize;
    private LinkedList<MusicItem> mTracks; 
    private int mUserRating;
    private String mUserComment;
    private int mListeners;
    private double mAvgRating;
    private boolean mIsShared;
    private boolean mIsSynced;
    
    private ArrayList<Integer> mRawTagsId;
    private HashMap<String, Object> mOptions;
    
    private LinkedList<MusicItem> mPlaylist;   

    public PlaylistItem() {
        mRawTagsId = new ArrayList<Integer>();
        mTracks = new LinkedList<MusicItem>();
        mOptions = new HashMap<String, Object>();
    }
    
    public PlaylistItem(long localId, String name) {
        mLocalId = localId;
        mTitle = name;
    }
    
    /**
     * 
     * @author marc
     *
     * TODO complete
     */
    public static class Builder {
        private long mLocalId; // Android sqlite
        private Date mLocalLastUpdated;
        private String mTitle;
        private boolean mCreatedByGS;
        private long mGSPLId; // GS database id
        private Date mGSCreated;
        private Date mGSUpdated;
        private Date mDateModified;
        private int mAuthorId;
        private String mAuthorName; // Not yet available
        private int mSize;
        private LinkedList<MusicItem> mTracks; 
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
         * 
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
         * 
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
        
        public Builder modBuilder(Date d) {
            this.mDateModified = d;
            return this;
        }
        
        public Builder modified(String u) {
            try {
                this.mDateModified = Uutils.stringToDate(u);
            } catch (ParseException e) {
                this.mDateModified = null;
                e.printStackTrace();
            }
            return this;
        }
        
        public Builder authorId(int id) {
            this.mAuthorId = id;
            return this;
        }
        
        public Builder authorName(String name) {
            this.mAuthorName = name;
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
                    ll.add(new MusicItem(t[i].localId, t[i].artist, t[i].title, t[i].playOrder));
                }
            }
            this.mTracks = ll;
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
        this.mLocalLastUpdated = builder.mLocalLastUpdated;
        this.mCreatedByGS = builder.mCreatedByGS;
        this.mGSPLId = builder.mGSPLId;
        this.mTitle = builder.mTitle;
        this.mGSCreated = builder.mGSCreated;
        this.mGSLastUpdated = builder.mGSUpdated;
        this.mAuthorId = builder.mAuthorId;
        this.mAuthorName = builder.mAuthorName;
        this.mGSSize = builder.mSize;
        this.mTracks = builder.mTracks;
        this.mUserRating = builder.mUserRating;
        this.mUserComment = builder.mUserComment;
        this.mListeners = builder.mListeners;
        this.mAvgRating = builder.mAvgRating;
        this.mIsShared = builder.mIsShared;
        this.mIsSynced = builder.mIsSynced;
    }
    

    @Deprecated
    public void addRawTags(ArrayList<Integer> seeds) {
        mRawTagsId.addAll(seeds);
    }


    public String getTitle() {
        return mTitle;
    }


    public void setTitle(String title) {
        this.mTitle = title;
    }


    public String getLastUpdated() {
        return mGSLastUpdated.toString();
    }


    public void setLastUpdated(Date lastUpdated) {
        this.mGSLastUpdated = lastUpdated;
    }
    
    public void setLastUpdated(String lastUpdated) {
        try {
            this.mGSLastUpdated = Uutils.stringToDate(lastUpdated);
        } catch (ParseException e) {
            this.mGSLastUpdated = null;
            e.printStackTrace();
        }
    }
    
    public void setCreated(String created) {
        try {
            this.mGSCreated = Uutils.stringToDate(created);
        } catch (ParseException e) {
            this.mGSCreated = null;
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


    public void setRawTagsId(ArrayList<Integer> rawTagsId) {
        this.mRawTagsId = rawTagsId;
    }


    public long getLocalId() {
        return mLocalId;
    }


    public long getPLId() {
        return mGSPLId;
    }


    public String getCreationTime() {
        return mGSCreated.toString();
    }


    public int getAuthorId() {
        return mAuthorId;
    }


    public String getAuthorName() {
        return mAuthorName;
    }


    public int getSize() {
        return mGSSize;
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


    @Override
    public int compareTo(AbstractItem another) {
        // TODO Auto-generated method stub
        return 0;
    }

}
