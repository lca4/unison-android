
package ch.epfl.unison.data;

import android.provider.BaseColumns;

/**
 * Constants used for the database.
 * 
 * @author marc
 */
final class Const {

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
    static final String LIBE_C_ID = C_ID;
    static final String LIBE_C_LOCAL_ID = "local_id";
    static final String LIBE_C_ARTIST = "artist";
    static final String LIBE_C_TITLE = "title";

    
    static final String TAG_TABLE_NAME = "tag"; // Prefix: TAG_
    // tags table fields here
    static final String TAG_C_ID = C_ID;
    static final String TAG_C_NAME = "name";
    static final String TAG_C_REMOTE_ID = "remote_id"; // may be useless
    static final String TAG_C_IS_CHECKED = "is_checked";
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
     */
    static final String PLAYLISTS_TABLE_NAME = "playlists";

    private Const() {
        // TODO Auto-generated constructor stub
    }
}
