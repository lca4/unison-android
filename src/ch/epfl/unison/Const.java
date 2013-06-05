
package ch.epfl.unison;

import java.util.HashMap;

/** Constants for the app. */
public final class Const {

    /** Preference keys. */
    public final class PrefKeys {
        public static final String EMAIL = "email";
        public static final String PASSWORD = "password";
        public static final String UID = "uid";
        public static final String LASTUPDATE = "lastupdate";
        public static final String NICKNAME = "nickname";
        public static final String HELPDIALOG = "helpdialog";
        public static final String HISTORY = "ghistory";
        public static final String GROUP_SUGGESTION = "gsuggestion";
        public static final String LOCATION_HISTORY = "lochist";

        private PrefKeys() {
        } // Non-instantiable.
    }

    /** Various other strings, e.g. keys for Intent extras. */
    public final class Strings {
        public static final String GID = "gid";
        // public static final String PLID = "plid";
        public static final String LOCAL_ID = "local_id";
        public static final String NAME = "name";
        public static final String TITLE = "title";
        public static final String LOGOUT = "logout";
        public static final String GROUP = "group"; // not sure if GID and NAME
                                                    // are used anymore
        public static final String CALLER = "caller";
        public static final String SONG_ARTIST_TITLE = "songat";
        public static final String GROUP_TO_CREATE_NAME = "gtcn";
        public static final String URI = "uri";
        public static final String TAG = "tag";
        public static final String SIZE = "size";

        public static final String UNISON_NFC_MIME_TYPE = "ch.epfl.unison:text/plain";

        private Strings() {
        } // Non-instantiable.
    }

    /** Various integer values. */
    public final class Integers {
        public static final int HUNDRED = 100;
        public static final int REMOVED_PLAYLISTS = -1;

        private Integers() {
        } // Non-instantiable.
    }

    private Const() {
    } // Non-instantiable.

    /**
     * Defines type of available seeds to be used when generating a playlist. To
     * each SeedType is associated a lower case label. Inspired form
     * http://javahowto.blogspot.ch/2008/04/java-enum-examples.html
     * 
     * @author marc
     */
    public enum SeedType {
        TAGS("tags".toLowerCase()),
        TRACKS("tracks".toLowerCase());

        private final String mLabel;

        private static HashMap<String, SeedType> smLabelToStatusMapping;

        private SeedType(String label) {
            this.mLabel = label;
        }

        /**
         * @param label
         * @return The SeedType corresponding to the label
         */
        public static SeedType getSeedType(String label) {
            if (smLabelToStatusMapping == null) {
                initMapping();
            }
            SeedType result = null;
            for (SeedType st : values()) {
                result = smLabelToStatusMapping.get(label.toLowerCase());
            }
            return result;
        }

        private static void initMapping() {
            smLabelToStatusMapping = new HashMap<String, Const.SeedType>();
            for (SeedType st : values()) {
                smLabelToStatusMapping.put(st.mLabel, st);
            }
        }

        public String getLabel() {
            return mLabel;
        }

        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("SeedType");
            sb.append("{label='").append(mLabel).append("'}");
            return sb.toString();
        }
    }

    /**
     * Defines available filters to be used when generating a playlist.
     * 
     * @author marc
     */
    public enum Filter {
        RATING_GEQ_4("rating>=4"),
        RATING_GEQ5("rating>=5");

        private final String mFilter;

        Filter(String filter) {
            this.mFilter = filter;
        }

        private String getValue() {
            return mFilter;
        }
    }

    /**
     * Defines available sorting criterion to be use d when generating a
     * playlist.
     * 
     * @author marc
     */
    public enum Sorting {
        RATING("rating".toLowerCase()),
        PROXIMITY("proximity".toLowerCase());

        private final String mSorting;

        Sorting(String filter) {
            this.mSorting = filter;
        }

        private String getValue() {
            return mSorting;
        }
    }
}
