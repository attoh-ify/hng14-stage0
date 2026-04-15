package hng14.stage0.nameclassifier.service;

import hng14.stage0.nameclassifier.dto.payload.CreatePayload;
import hng14.stage0.nameclassifier.dto.success.CreateSuccessResponse;
import hng14.stage0.nameclassifier.dto.success.GetAllSuccessResponse;
import hng14.stage0.nameclassifier.dto.success.GetSuccessResponse;

public interface ProfileService {
    CreateSuccessResponse create(CreatePayload payload);
    GetSuccessResponse get(String profileId);
    GetAllSuccessResponse getAll(String gender, String countryId, String ageGroup);
    void delete(String profileId);
}
