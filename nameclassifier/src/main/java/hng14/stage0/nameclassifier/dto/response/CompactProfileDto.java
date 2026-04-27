package hng14.stage0.nameclassifier.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import hng14.stage0.nameclassifier.enums.AgeGroup;

public record CompactProfileDto(
        String id,
        String name,
        String gender,
        int age,
        @JsonProperty("age_group")
        AgeGroup ageGroup,
        @JsonProperty("country_id")
        String countryId
) {
}