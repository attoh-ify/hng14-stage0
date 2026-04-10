package hng14.stage0.nameclassifier.dto.response;

public record SuccessResponse (
        String status,
        ClassifyDataResponse data
) {
}
