package hng14.stage0.nameclassifier.dto.success;

import hng14.stage0.nameclassifier.dto.response.ProfileDto;

public record GetSuccessResponse(
        String status,
        ProfileDto data
) {
}
