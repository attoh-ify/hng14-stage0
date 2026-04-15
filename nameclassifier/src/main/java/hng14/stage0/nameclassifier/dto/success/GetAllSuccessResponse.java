package hng14.stage0.nameclassifier.dto.success;

import hng14.stage0.nameclassifier.dto.response.CompactProfileDto;

import java.util.List;

public record GetAllSuccessResponse(
        String status,
        int count,
        List<CompactProfileDto> data
) {
}
