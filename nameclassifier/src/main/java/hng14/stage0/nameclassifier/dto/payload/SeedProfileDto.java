package hng14.stage0.nameclassifier.dto.payload;

public record SeedProfileDto(
        String id,
        String name,
        String gender,
        double gender_probability,
        int age,
        String age_group,
        String country_id,
        String country_name,
        double country_probability,
        String created_at
) {}
