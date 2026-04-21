package hng14.stage0.nameclassifier.dto.payload;

import hng14.stage0.nameclassifier.dto.response.AgeGroup;

public record ParsedSearchQuery(
        String gender,
        AgeGroup ageGroup,
        Integer minAge,
        Integer maxAge,
        String countryId
) {
}