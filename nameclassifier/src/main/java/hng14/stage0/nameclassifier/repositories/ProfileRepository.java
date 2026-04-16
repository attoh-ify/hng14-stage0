package hng14.stage0.nameclassifier.repositories;

import hng14.stage0.nameclassifier.dto.response.AgeGroup;
import hng14.stage0.nameclassifier.entities.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, String> {
    Optional<Profile> findByName(String normalizedName);
    @Query("""
        SELECT p FROM Profile p
        WHERE (:gender IS NULL OR p.gender = :gender)
        AND (:countryId IS NULL OR p.countryId = :countryId)
        AND (:ageGroup IS NULL OR p.ageGroup = :ageGroup)
    """)
    List<Profile> findAllWithFilters(
            @Param("gender") String gender,
            @Param("countryId") String countryId,
            @Param("ageGroup") AgeGroup ageGroup
    );
}
