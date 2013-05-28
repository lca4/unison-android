
package ch.epfl.unison.api;

import ch.epfl.unison.data.PlaylistItem;
import ch.epfl.unison.data.TagItem;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * POJOs for JSON serialization / deserialization.
 * 
 * @author lum
 */
public abstract class JsonStruct {

    /** Error message from the server. */
    public static class Error extends JsonStruct {

        public Integer error;
        public String message;
    }

    /** Success message from the server. */
    public static class Success extends JsonStruct {

        public Boolean success;
    }

    /** Information about a user (used in both directions). */
    public static class User extends JsonStruct {

        public Long uid;
        public String nickname;
        public String email;
        public String password;
        public Long gid;
        public Integer score;
        public Boolean predicted; // or isPredicted?
    }
    
    /** Cluster as used on the server to regroup users. */
    public static class Cluster extends JsonStruct {
        
        public Long cid;
        public Double lat;
        public Double lon;
        public Long gid;
    }
    
    /** Group Suggestion from the server. */
    public static class GroupSuggestion extends JsonStruct {
        
        public boolean suggestion;
        public Cluster cluster;
        
        public Group group;
        //Note that the users are not the same as the Users Field of the group as it
        //may also contain users that are not in the group.
        public String[] users;
    }

    /** Information about a track (used in both directions). */
    public static class Track extends JsonStruct {

        public String artist;
        public String title;
        public String image;
        public Integer localId;
        public Integer rating;
        public Integer playOrder;

        public Track() {
        }

        public Track(int id, String a, String t) {
            localId = id;
            artist = a;
            title = t;
        }
    }

    /** Information about a playlist (sent by server). */
    public static class TracksList extends JsonStruct {

        public String playlistId;
        public Track[] tracks;
    }

    /** Information about a group (used in both directions). */
    public static class Group extends JsonStruct implements Serializable {

        /**
		 * 
		 */
        private static final long serialVersionUID = 1L;
        public Long gid;
        public String name;
        public Track track;
        public Float distance;
        public User master;
        public User[] users;
        public Integer nbUsers;
        public boolean password = false;
        
        //Linked to automatic groups. We use default values for backwards compatibility.
        public boolean automatic = false;
        public Double lat = 0.0;
        public Double lon = 0.0;
        
    }

    /** List of groups. */
    public static class GroupsList extends JsonStruct {

        public Group[] groups;
    }

    /** Notification of addition / removal of track on the device. */
    public static class Delta extends JsonStruct {

        public static final String TYPE_PUT = "PUT";
        public static final String TYPE_DELETE = "DELETE";

        public String type;
        public Track entry;

        public Delta() {
        }

        public Delta(String t, int localId, String artist, String title) {
            this.type = t;
            this.entry = new Track(localId, artist, title);
        }
    }

    /** Information about a playlist (used in both directions). */
    public static class PlaylistJS extends JsonStruct {

        public int gsPlaylistId;
        public String gsCreationTime; // ISO format
        public String gsUpdateTime; // ISO format
        public String title;
        public String image; // Not used for now
        public Integer authorId;
        public String authorName;
        public Integer gsSize;
        public Track[] tracks;
        public Integer gsListeners;
        public Double gsAvgRating;
        public Boolean gsIsShared;
        public Boolean gsIsSynced;
        public Integer gsUserRating;
        public String gsUserComment; // Not used for now

        public PlaylistItem toObject() {
            // TODO complete
            return new PlaylistItem.Builder().plId(gsPlaylistId).title(title).tracks(tracks)
                    .size(gsSize).authorId(authorId).created(gsCreationTime).gsUpdated(gsUpdateTime)
                    .listeners(gsListeners).build();
        }

    }

    /** List of playlists. */
    public static class PlaylistsList extends JsonStruct {

        public PlaylistJS[] playlists;

        public ArrayList<PlaylistItem> toObject() {
            if (playlists != null) {
                ArrayList<PlaylistItem> al = new ArrayList<PlaylistItem>();
                for (int i = 0; i < playlists.length; i++) {
                    al.add(playlists[i].toObject());
                }
                return al;
            }
            return null;
        }
        
        public boolean isEmtpy() {
            if (playlists.length == 0) {
                return true;
            }
            return false;
        }
    }

    /** Information about a tag (used in both directions). */
    public static class Tag extends JsonStruct {

        // public int tid;
        public String name;
        public Long refId;

        public TagItem getTagItem() {
            return new TagItem(name, refId);
        }
    }

    /** List of tags. */
    public static class TagsList extends JsonStruct {

        public Tag[] tags;
    }
}
