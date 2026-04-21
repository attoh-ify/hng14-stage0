package hng14.stage0.nameclassifier.mappers.impl;

import hng14.stage0.nameclassifier.dto.response.CompactProfileDto;
import hng14.stage0.nameclassifier.dto.response.ProfileDto;
import hng14.stage0.nameclassifier.entities.Profile;
import hng14.stage0.nameclassifier.mappers.ProfileMapper;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapperImpl implements ProfileMapper {
    @Override
    public Profile fromDto(ProfileDto profileDto) {
        if (profileDto == null) return null;
        return new Profile(
                profileDto.id(),
                profileDto.name(),
                profileDto.gender(),
                profileDto.genderProbability(),
                profileDto.age(),
                profileDto.ageGroup(),
                profileDto.countryId(),
                profileDto.countryName(),
                profileDto.countryProbability(),
                profileDto.createdAt()
        );
    }

    @Override
    public ProfileDto toDto(Profile profile) {
        if (profile == null) return null;
        return new ProfileDto(
                profile.getId(),
                profile.getName(),
                profile.getGender(),
                profile.getGenderProbability(),
                profile.getAge(),
                profile.getAgeGroup(),
                profile.getCountryId(),
                profile.getCountryName(),
                profile.getCountryProbability(),
                profile.getCreatedAt()
        );
    }

    @Override
    public CompactProfileDto toCompactDto(Profile profile) {
        if (profile == null) return null;
        return new CompactProfileDto(
                profile.getId(),
                profile.getName(),
                profile.getGender(),
                profile.getAge(),
                profile.getAgeGroup(),
                profile.getCountryId()
        );
    }
}
