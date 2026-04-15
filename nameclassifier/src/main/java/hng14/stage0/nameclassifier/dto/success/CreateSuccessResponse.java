package hng14.stage0.nameclassifier.dto.success;

import hng14.stage0.nameclassifier.dto.response.ProfileDto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateSuccessResponse(
        String status,
        String message,
        ProfileDto data
) {
}
