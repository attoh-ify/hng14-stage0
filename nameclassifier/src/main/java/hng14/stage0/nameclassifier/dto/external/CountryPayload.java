package hng14.stage0.nameclassifier.dto.external;

public record CountryPayload(
        String country_id,
        double probability
) {
}
