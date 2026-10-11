package model;

/** The vault owner. Stores only an encoded form of the master password. */
public class User {
    private final String username;
    private final String encodedPassword;

    public User(String username, String encodedPassword) {
        this.username = username;
        this.encodedPassword = encodedPassword;
    }

    /** Builds a user from a plain password. */
    public static User fromPlain(String username, String plainPassword) {
        return new User(username, encode(plainPassword));
    }

    public String getUsername() {
        return username;
    }

    public String getEncodedPassword() {
        return encodedPassword;
    }

    public boolean matches(String plainPassword) {
        return encodedPassword.equals(encode(plainPassword));
    }

    /**
     * Simple position-based scramble. This is for learning only;
     * real applications must use a proper password hashing library.
     */
    public static String encode(String raw) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            int value = ((raw.charAt(i) + 7) * (i + 3)) ^ 5;
            sb.append(value).append('-');
        }
        return sb.toString();
    }
}
