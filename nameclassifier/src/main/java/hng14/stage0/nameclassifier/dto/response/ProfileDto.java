package hng14.stage0.nameclassifier.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProfileDto(
        String id,
        String name,
        String gender,
        @JsonProperty("gender_probability")
        double genderProbability,
        @JsonProperty("sample_size")
        long sampleSize,
        int age,
        @JsonProperty("age_group")
        AgeGroup ageGroup,
        @JsonProperty("country_id")
        String countryId,
        @JsonProperty("country_probability")
        double countryProbability,
        @JsonProperty("created_at")
        String createdAt
) {
}
