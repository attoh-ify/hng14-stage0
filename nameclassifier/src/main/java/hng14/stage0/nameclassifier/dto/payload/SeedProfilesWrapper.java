package hng14.stage0.nameclassifier.dto.payload;

import java.util.List;

public record SeedProfilesWrapper(
        List<SeedProfileDto> profiles
) {
}