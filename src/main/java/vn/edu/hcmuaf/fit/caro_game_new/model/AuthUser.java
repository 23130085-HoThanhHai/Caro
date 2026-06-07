package vn.edu.hcmuaf.fit.caro_game_new.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class AuthUser implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final long id;
    private final String username;
    private final String displayName;
    private final UserRole role;
    private final String avatarUrl;

    public AuthUser(long id, String username, String displayName, UserRole role, String avatarUrl) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username");
        this.displayName = displayName;
        this.role = role == null ? UserRole.PLAYER : role;
        this.avatarUrl = avatarUrl;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String effectiveName() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return username;
    }
}
