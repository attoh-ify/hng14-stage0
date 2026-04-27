package hng14.stage0.nameclassifier.dto.success;

import com.fasterxml.jackson.annotation.JsonProperty;
import hng14.stage0.nameclassifier.dto.response.PaginationLinks;
import hng14.stage0.nameclassifier.dto.response.ProfileDto;

import java.util.List;

public record GetAllSuccessResponse(
        String status,
        int page,
        int limit,
        long total,

        @JsonProperty("total_pages")
        int totalPages,

        PaginationLinks links,
        List<ProfileDto> data
) {
}