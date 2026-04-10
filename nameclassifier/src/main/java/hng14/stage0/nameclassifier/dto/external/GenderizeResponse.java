package hng14.stage0.nameclassifier.dto.external;

public record GenderizeResponse (
        long count,
        String name,
        String gender,
        double probability
) {
}
