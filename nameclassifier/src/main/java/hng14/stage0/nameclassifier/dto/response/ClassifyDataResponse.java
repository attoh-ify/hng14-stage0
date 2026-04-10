package hng14.stage0.nameclassifier.dto.response;

public record ClassifyDataResponse (
        String name,
        String gender,
        double probability,
        long sample_size,
        boolean is_confident,
        String processed_at
) {
}
