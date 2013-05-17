
package ch.epfl.unison.data;

import android.provider.BaseColumns;

/**
 * Constants used for the database.
 * 
 * @author marc
 */
final class ConstDB {

    // Global definitions
    static final String DATABASE_NAME = "unison.db";
    static final int DATABASE_VERSION = 1;
    
    // Global aliases
    static final int TRUE = 1;
    static final int FALSE = 0;
    
    // Global fields
    static final String C_ID = BaseColumns._ID;
    static final String C_IS_CHECKED = "is_checked";

    
    static final String LIBE_TABLE_NAME = "lib_entry"; // Prefix: LIBE_
    // lib_entry table fields here
//    static final String LIBE_C_ID = C_ID;
    static final String LIBE_C_LOCAL_ID = "local_id";
    static final String LIBE_C_ARTIST = "artist";
    static final String LIBE_C_TITLE = "title";

    
    static final String TAG_TABLE_NAME = "tag"; // Prefix: TAG_
    // tags table fields here
//    static final String TAG_C_ID = C_ID;
    static final String TAG_C_NAME = "name";
    static final String TAG_C_REMOTE_ID = "gs_tag_id"; // may be useless
//    static final String TAG_C_IS_CHECKED = "is_checked";
    // index
    static final String TAG_INDEX_NAME = "tag_name_idx";

    
    static final String MOOD_TABLE_NAME = "mood"; // Prefix: MOOD_
    // moods table fields here
    static final String MOOD_C_ID = BaseColumns._ID;
    static final String MOOD_C_NAME = "name";

    
    static final String MOOD_TAG_TABLE_NAME = "moods_tags"; // Prefix: MOTA_
    // moods-tags table fields here
    static final String MOTA_C_MOOD_ID = "mood_id";
    static final String MOTA_C_TAG_ID = "tag_id";
    
    
    /*
     * The playlists table will just be used to store additional data about the
     * playlists that has to be persistent. These infos include for e.g. isSynced
     * 
     * Be careful, the playlists can modified outside of GroupStreamer, since they're stored in the 
     * shared android playlists database!
     */
    static final String PLAYLISTS_TABLE_NAME = "playlists"; // Prefix: PLYL_
//    static final String PLYL_C_ID = C_ID;
    static final String PLYL_C_LOCAL_ID = "local_id";
    static final String PLYL_C_GS_SIZE = "gs_size";
    //static final String PLYL_C_LOCAL_UPDATE_TIME = "update_time"; // =PlaylistsColumns.DATE_MODIFIED
    static final String PLYL_C_CREATED_BY_GS = "created_by_gs"; // was PL created using GS? // Maybe useless, because quiet obvious
    static final String PLYL_C_GS_ID = "gs_playlist_id"; // if created by GroupStreamer
    static final String PLYL_C_GS_CREATION_TIME = "gs_creation_time"; // if created by GS
    // Update the android playlists only if requested, thus track last update
    static final String PLYL_C_GS_UPDATE_TIME = "gs_update_time"; // if created by GS
    static final String PLYL_C_GS_AVG_RATING = "gs_avg_rating";
    static final String PLYL_C_GS_IS_SHARED = "gs_is_shared";
    static final String PLYL_C_GS_IS_SYNCED = "gs_is_synced";
    static final String PLYL_C_GS_USER_RATING = "gs_user_rating";
    static final String PLYL_C_GS_USER_COMMENT = "gs_user_comment";
    
    // Indexes
    static final String PLYL_INDEX_GS_ID = "plyl_gs_playlist_id_idx";
    static final String PLYL_INDEX_GS_SIZE = "plyl_gs_size_idx";
    static final String PLYL_INDEX_GS_AVG_RATING = "plyl_gs_avg_rating_idx";
    static final String PLYL_INDEX_GS_USER_RATING = "plyl_gs_user_rating_idx";
    

    // Const class can't be instanciated
    private ConstDB() {
        // TODO Auto-generated constructor stub
    }
}
