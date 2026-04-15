package hng14.stage0.nameclassifier.dto.external;

import java.util.List;

public record NationalizeResponse(
        long count,
        String name,
        List<CountryPayload> country
) {
}
