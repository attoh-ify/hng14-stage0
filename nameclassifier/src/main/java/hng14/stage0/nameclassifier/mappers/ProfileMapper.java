package hng14.stage0.nameclassifier.mappers;

import hng14.stage0.nameclassifier.dto.response.CompactProfileDto;
import hng14.stage0.nameclassifier.dto.response.ProfileDto;
import hng14.stage0.nameclassifier.entities.Profile;

public interface ProfileMapper {
    Profile fromDto(ProfileDto profileDto);
    ProfileDto toDto(Profile profile);
    CompactProfileDto toCompactDto(Profile profile);
}
