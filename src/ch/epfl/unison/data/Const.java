
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

    private Const() {
        // TODO Auto-generated constructor stub
    }
}
