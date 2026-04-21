package hng14.stage0.nameclassifier.service;

import hng14.stage0.nameclassifier.dto.payload.CreatePayload;
import hng14.stage0.nameclassifier.dto.success.CreateSuccessResponse;
import hng14.stage0.nameclassifier.dto.success.GetAllSuccessResponse;
import hng14.stage0.nameclassifier.dto.success.GetSuccessResponse;

public interface ProfileService {
    CreateSuccessResponse create(CreatePayload payload);
    GetSuccessResponse get(String profileId);
    GetAllSuccessResponse getAll(
            String gender,
            String countryId,
            String ageGroup,
            Integer minAge,
            Integer maxAge,
            Double minGenderProbability,
            Double minCountryProbability,
            String sortBy,
            String order,
            Integer page,
            Integer limit
    );
    GetAllSuccessResponse search(String query, Integer page, Integer limit);
    void delete(String profileId);
}
