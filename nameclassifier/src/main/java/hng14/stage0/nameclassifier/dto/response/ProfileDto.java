package hng14.stage0.nameclassifier.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ProfileDto(
        String id,
        String name,
        String gender,
        @JsonProperty("gender_probability")
        double genderProbability,
        int age,
        @JsonProperty("age_group")
        AgeGroup ageGroup,
        @JsonProperty("country_id")
        String countryId,
        @JsonProperty("country_name")
        String countryName,
        @JsonProperty("country_probability")
        double countryProbability,
        @JsonProperty("created_at")
        Instant createdAt
) {
}
