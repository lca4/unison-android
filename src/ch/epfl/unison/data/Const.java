
package ch.epfl.unison.data;

import android.provider.BaseColumns;

/**
 * Constants used for the database.
 * 
 * @author marc
 */
final class Const {

    static final String DATABASE_NAME = "unison.db";
    static final int DATABASE_VERSION = 1;

    static final String LIBE_TABLE_NAME = "lib_entry"; // Prefix: LIBE_
    // lib_entry table fields here
    static final String LIBE_C_ID = BaseColumns._ID;
    static final String LIBE_C_LOCAL_ID = "local_id";
    static final String LIBE_C_ARTIST = "artist";
    static final String LIBE_C_TITLE = "title";

    static final String TAGS_TABLE_NAME = "tags"; // Prefix: TAGS_
    // tags table fields here
    static final String TAGS_C_ID = BaseColumns._ID;
    static final String TAGS_C_NAME = "name";
    static final String TAGS_C_REMOTE_ID = "remote_id";
    
    static final String MOODS_TABLE_NAME = "mood"; // Prefix: MOOD_
    static final String MOOD_C_ID = BaseColumns._ID;
    static final String MOOD_C_NAME = "name";
    
    static final String MOODS_TAGS_TABLE_NAME = "moods_tags"; // Prefix: MOTA_
    static final String MOTA_C_MOOD_ID = "mood_id";
    static final String MOTA_C_TAG_ID = "tag_id";

    private Const() {
        // TODO Auto-generated constructor stub
    }
}
