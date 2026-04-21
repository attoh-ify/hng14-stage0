package hng14.stage0.nameclassifier.dto.success;

import hng14.stage0.nameclassifier.dto.response.ProfileDto;

import java.util.List;

public record GetAllSuccessResponse(
        String status,
        int page,
        int limit,
        long total,
        List<ProfileDto> data
) {}
