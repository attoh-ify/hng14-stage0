package hng14.stage0.nameclassifier.service.impl;

import com.github.f4b6a3.uuid.UuidCreator;
import hng14.stage0.nameclassifier.client.AgifyClient;
import hng14.stage0.nameclassifier.client.GenderizeClient;
import hng14.stage0.nameclassifier.client.NationalizeClient;
import hng14.stage0.nameclassifier.dto.external.AgifyResponse;
import hng14.stage0.nameclassifier.dto.external.CountryPayload;
import hng14.stage0.nameclassifier.dto.external.GenderizeResponse;
import hng14.stage0.nameclassifier.dto.external.NationalizeResponse;
import hng14.stage0.nameclassifier.dto.payload.CreatePayload;
import hng14.stage0.nameclassifier.dto.response.AgeGroup;
import hng14.stage0.nameclassifier.dto.response.CompactProfileDto;
import hng14.stage0.nameclassifier.dto.success.CreateSuccessResponse;
import hng14.stage0.nameclassifier.dto.success.GetAllSuccessResponse;
import hng14.stage0.nameclassifier.dto.success.GetSuccessResponse;
import hng14.stage0.nameclassifier.entities.Profile;
import hng14.stage0.nameclassifier.exception.BadRequestException;
import hng14.stage0.nameclassifier.exception.NotFoundException;
import hng14.stage0.nameclassifier.exception.UnprocessableEntityException;
import hng14.stage0.nameclassifier.exception.UpstreamServiceException;
import hng14.stage0.nameclassifier.mappers.ProfileMapper;
import hng14.stage0.nameclassifier.repositories.ProfileRepository;
import hng14.stage0.nameclassifier.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ProfileServiceImpl implements ProfileService {
    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);
    private final GenderizeClient genderizeClient;
    private final AgifyClient agifyClient;
    private final NationalizeClient nationalizeClient;
    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;

    public ProfileServiceImpl(GenderizeClient genderizeClient, AgifyClient agifyClient, NationalizeClient nationalizeClient, ProfileRepository profileRepository, ProfileMapper profileMapper) {
        this.genderizeClient = genderizeClient;
        this.agifyClient = agifyClient;
        this.nationalizeClient = nationalizeClient;
        this.profileRepository = profileRepository;
        this.profileMapper = profileMapper;
    }

    private AgeGroup resolveAgeGroup(int age) {
        if (age <= 12) return AgeGroup.child;
        if (age <= 19) return AgeGroup.teenager;
        if (age <= 59) return AgeGroup.adult;
        return AgeGroup.senior;
    }

    @Override
    public CreateSuccessResponse create(CreatePayload payload) {
        if (payload == null || payload.name() == null || payload.name().trim().isEmpty()) {
            log.debug("Missing or empty name parameter");
            throw new BadRequestException("Missing or empty name parameter");
        }

        String trimmedName = payload.name().trim();
        String normalizedName = payload.name().trim().toLowerCase();

        if (!trimmedName.matches("^[a-zA-Z]+$")) {
            log.debug("name is not a string");
            throw new UnprocessableEntityException("name is not a string");
        }

        Optional<Profile> exists = profileRepository.findByName(normalizedName);
        if (exists.isPresent()) {
            return new CreateSuccessResponse("success", "Profile already exists", profileMapper.toDto(exists.get()));
        }

        GenderizeResponse genderizeResponse = genderizeClient.classifyName(trimmedName);
        AgifyResponse agifyResponse = agifyClient.agifyName(trimmedName);
        NationalizeResponse nationalizeResponse = nationalizeClient.nationalizeName(trimmedName);

        if (genderizeResponse.gender() == null || genderizeResponse.count() == 0) {
            throw new UpstreamServiceException("Genderize returned an invalid response");
        }

        if (agifyResponse.age() == null) {
            throw new UpstreamServiceException("Agify returned an invalid response");
        }

        if (nationalizeResponse.country() == null || nationalizeResponse.country().isEmpty()) {
            throw new UpstreamServiceException("Nationalize returned an invalid response");
        }

        String id = UuidCreator.getTimeOrderedEpoch().toString();
        AgeGroup ageGroup = resolveAgeGroup(agifyResponse.age());
        CountryPayload bestCountry = nationalizeResponse.country().stream()
                .max(Comparator.comparingDouble(CountryPayload::probability))
                .orElseThrow(() -> new UpstreamServiceException("Nationalize returned an invalid response"));

        Profile newProfile = new Profile(
                id,
                normalizedName,
                genderizeResponse.gender(),
                genderizeResponse.probability(),
                genderizeResponse.count(),
                agifyResponse.age(),
                ageGroup,
                bestCountry.country_id(),
                bestCountry.probability(),
                Instant.now().toString()
        );
        profileRepository.save(newProfile);
        return new CreateSuccessResponse("success", null, profileMapper.toDto(newProfile));
    }

    @Override
    public GetSuccessResponse get(String profileId) {
        Optional<Profile> profile = profileRepository.findById(profileId);
        if (profile.isEmpty()) {
            throw new NotFoundException("Profile not found");
        }
        return new GetSuccessResponse("success", profileMapper.toDto(profile.get()));
    }

    @Override
    public GetAllSuccessResponse getAll(String gender, String countryId, String ageGroup) {
        String normalizedGender = (gender == null || gender.isBlank()) ? null : gender.trim().toLowerCase();
        String normalizedCountryId = (countryId == null || countryId.isBlank()) ? null : countryId.trim().toUpperCase();

        AgeGroup parsedAgeGroup = null;

        if (ageGroup != null && !ageGroup.isBlank()) {
            try {
                parsedAgeGroup = AgeGroup.valueOf(ageGroup.trim().toLowerCase());
            } catch (Exception e) {
                throw new UnprocessableEntityException("Invalid age_group");
            }
        }

        List<Profile> profiles = profileRepository.findAllWithFilters(
                normalizedGender,
                normalizedCountryId,
                parsedAgeGroup
        );

        List<CompactProfileDto> data = profiles.stream()
                .map(profileMapper::toCompactDto)
                .toList();

        return new GetAllSuccessResponse("success", profiles.size(), data);
    }

    @Override
    public void delete(String profileId) {
        if (!profileRepository.existsById(profileId)) {
            throw new NotFoundException("Profile not found");
        }
        profileRepository.deleteById(profileId);
    }
}
