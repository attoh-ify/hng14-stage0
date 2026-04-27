package hng14.stage0.nameclassifier.repositories;

import hng14.stage0.nameclassifier.entities.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, String> {
    Optional<AppUser> findByGithubId(String githubId);
}