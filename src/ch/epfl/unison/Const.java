package ch.epfl.unison;

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

        private PrefKeys() { }  // Non-instantiable.
    }

    /** Various other strings, e.g. keys for Intent extras. */
    public final class Strings {
        public static final String GID = "gid";
        public static final String NAME = "name";
        public static final String LOGOUT = "logout";
        public static final String GROUP = "group"; //not sure if GID and NAME are used anymore
        public static final String CALLER = "caller";

        private Strings() { }  // Non-instantiable.
    }

    private Const() { }  // Non-instantiable.
}
