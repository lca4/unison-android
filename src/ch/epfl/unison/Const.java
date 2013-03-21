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

        private PrefKeys() { }  // Non-instantiable.
    }

    /** Various other strings, e.g. keys for Intent extras. */
    public final class Strings {
        public static final String GID = "gid";
        public static final String NAME = "name";
        public static final String LOGOUT = "logout";

        private Strings() { }  // Non-instantiable.
    }

    private Const() { }  // Non-instantiable.
}