
import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;

public final class PasswordHasher {
    private PasswordHasher() {
    }

    public static String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("password is null");
        }
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public static boolean verify(String password, String hash) {
        if (password == null || hash == null || hash.isBlank()) {
            return false;
        }
        return BCrypt.checkpw(password, hash);
    }

    public static String hashRandomSecret() {
        String random = UUID.randomUUID() + ":" + UUID.randomUUID();
        return hash(random);
    }
}
