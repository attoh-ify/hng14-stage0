package hng14.stage0.nameclassifier.entities;

import hng14.stage0.nameclassifier.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "github_id", nullable = false, unique = true)
    private String githubId;

    @Column(nullable = false)
    private String username;

    private String email;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Override
    public String toString() {
        return "AppUser{" +
                "id='" + id + '\'' +
                ", githubId='" + githubId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", role=" + role +
                ", active=" + active +
                ", lastLoginAt=" + lastLoginAt +
                ", createdAt=" + createdAt +
                '}';
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.analyst;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}