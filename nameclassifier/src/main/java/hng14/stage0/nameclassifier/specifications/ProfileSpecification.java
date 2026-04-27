package hng14.stage0.nameclassifier.specifications;

import hng14.stage0.nameclassifier.enums.AgeGroup;
import hng14.stage0.nameclassifier.entities.Profile;
import org.springframework.data.jpa.domain.Specification;

public class ProfileSpecification {

    public static Specification<Profile> hasGender(String gender) {
        return (root, query, cb) ->
                (gender == null || gender.isBlank())
                        ? null
                        : cb.equal(cb.lower(root.get("gender")), gender.toLowerCase());
    }

    public static Specification<Profile> hasCountryId(String countryId) {
        return (root, query, cb) ->
                (countryId == null || countryId.isBlank())
                        ? null
                        : cb.equal(cb.lower(root.get("countryId")), countryId.toLowerCase());
    }

    public static Specification<Profile> hasAgeGroup(AgeGroup ageGroup) {
        return (root, query, cb) ->
                ageGroup == null
                        ? null
                        : cb.equal(root.get("ageGroup"), ageGroup);
    }

    public static Specification<Profile> hasMinAge(Integer minAge) {
        return (root, query, cb) ->
                minAge == null
                        ? null
                        : cb.greaterThanOrEqualTo(root.get("age"), minAge);
    }

    public static Specification<Profile> hasMaxAge(Integer maxAge) {
        return (root, query, cb) ->
                maxAge == null
                        ? null
                        : cb.lessThanOrEqualTo(root.get("age"), maxAge);
    }

    public static Specification<Profile> hasMinGenderProbability(Double minGenderProbability) {
        return (root, query, cb) ->
                minGenderProbability == null
                        ? null
                        : cb.greaterThanOrEqualTo(root.get("genderProbability"), minGenderProbability);
    }

    public static Specification<Profile> hasMinCountryProbability(Double minCountryProbability) {
        return (root, query, cb) ->
                minCountryProbability == null
                        ? null
                        : cb.greaterThanOrEqualTo(root.get("countryProbability"), minCountryProbability);
    }
}