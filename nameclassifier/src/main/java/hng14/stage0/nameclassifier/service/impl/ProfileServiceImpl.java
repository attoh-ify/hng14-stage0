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
import hng14.stage0.nameclassifier.dto.payload.ParsedSearchQuery;
import hng14.stage0.nameclassifier.dto.response.PaginationLinks;
import hng14.stage0.nameclassifier.enums.AgeGroup;
import hng14.stage0.nameclassifier.dto.response.ProfileDto;
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
import hng14.stage0.nameclassifier.specifications.ProfileSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static hng14.stage0.nameclassifier.utils.Helpers.*;

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

    @Override
    public CreateSuccessResponse create(CreatePayload payload) {
        if (payload == null || payload.name() == null || payload.name().trim().isEmpty()) {
            log.debug("Missing or empty name parameter");
            throw new BadRequestException("Missing or empty name parameter");
        }

        String trimmedName = payload.name().trim();

        if (!trimmedName.matches("^[a-zA-Z]+(?:\\s+[a-zA-Z]+)*$")) {
            throw new UnprocessableEntityException("name is not a string");
        }

        Optional<Profile> exists = profileRepository.findByNameIgnoreCase(trimmedName);
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

        String countryName = "";
        for (Map.Entry<String, String> entry : COUNTRY_MAP.entrySet()) {
            if (bestCountry.country_id().equals(entry.getValue())) {
                countryName = entry.getKey();
            }
        }
        if (countryName.isEmpty()) {
            throw new UpstreamServiceException("Nationalize returned an invalid response");
        }

        Profile newProfile = new Profile(
                id,
                trimmedName,
                genderizeResponse.gender(),
                genderizeResponse.probability(),
                agifyResponse.age(),
                ageGroup,
                bestCountry.country_id(),
                countryName,
                bestCountry.probability(),
                Instant.now()
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
    public GetAllSuccessResponse getAll(
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
    ) {
        return getAllInternal(
                "/api/profiles",
                gender,
                countryId,
                ageGroup,
                minAge,
                maxAge,
                minGenderProbability,
                minCountryProbability,
                sortBy,
                order,
                page,
                limit
        );
    }

    @Override
    public GetAllSuccessResponse search(String query, Integer page, Integer limit) {
        ParsedSearchQuery parsed = parse(query);

        return getAllInternal(
                "/api/profiles/search",
                parsed.gender(),
                parsed.countryId(),
                parsed.ageGroup() == null ? null : parsed.ageGroup().name(),
                parsed.minAge(),
                parsed.maxAge(),
                null,
                null,
                "created_at",
                "asc",
                page,
                limit
        );
    }

    @Override
    public void delete(String profileId) {
        if (!profileRepository.existsById(profileId)) {
            throw new NotFoundException("Profile not found");
        }
        profileRepository.deleteById(profileId);
    }

    @Override
    public String exportProfiles(
            String format,
            String gender,
            String countryId,
            String ageGroup,
            Integer minAge,
            Integer maxAge,
            Double minGenderProbability,
            Double minCountryProbability,
            String sortBy,
            String order
    ) {
        if (format == null || !format.equalsIgnoreCase("csv")) {
            throw new UnprocessableEntityException("Invalid query parameters");
        }

        validateQueryParams(
                minAge,
                maxAge,
                minGenderProbability,
                minCountryProbability,
                null,
                null
        );

        Sort sort = buildSort(sortBy, order);

        Specification<Profile> spec = Specification.allOf(
                ProfileSpecification.hasGender(gender),
                ProfileSpecification.hasCountryId(countryId),
                ProfileSpecification.hasAgeGroup(parseAgeGroupParam(ageGroup)),
                ProfileSpecification.hasMinAge(minAge),
                ProfileSpecification.hasMaxAge(maxAge),
                ProfileSpecification.hasMinGenderProbability(minGenderProbability),
                ProfileSpecification.hasMinCountryProbability(minCountryProbability)
        );

        List<Profile> profiles = profileRepository.findAll(spec, sort);

        StringBuilder csv = new StringBuilder();

        csv.append("id,name,gender,gender_probability,age,age_group,country_id,country_name,country_probability,created_at\n");

        for (Profile profile : profiles) {
            csv.append(escapeCsv(profile.getId())).append(",");
            csv.append(escapeCsv(profile.getName())).append(",");
            csv.append(escapeCsv(profile.getGender())).append(",");
            csv.append(profile.getGenderProbability()).append(",");
            csv.append(profile.getAge()).append(",");
            csv.append(profile.getAgeGroup()).append(",");
            csv.append(escapeCsv(profile.getCountryId())).append(",");
            csv.append(escapeCsv(profile.getCountryName())).append(",");
            csv.append(profile.getCountryProbability()).append(",");
            csv.append(profile.getCreatedAt()).append("\n");
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        boolean mustQuote = value.contains(",") || value.contains("\"") || value.contains("\n");

        String escaped = value.replace("\"", "\"\"");

        return mustQuote ? "\"" + escaped + "\"" : escaped;
    }

    private GetAllSuccessResponse getAllInternal(
            String basePath,
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
    ) {
        validateQueryParams(minAge, maxAge, minGenderProbability, minCountryProbability, page, limit);

        int resolvedPage = (page == null) ? 1 : page;
        int resolvedLimit = (limit == null) ? 10 : limit;

        Sort sort = buildSort(sortBy, order);
        Pageable pageable = PageRequest.of(resolvedPage - 1, resolvedLimit, sort);

        Specification<Profile> spec = Specification.allOf(
                ProfileSpecification.hasGender(gender),
                ProfileSpecification.hasCountryId(countryId),
                ProfileSpecification.hasAgeGroup(parseAgeGroupParam(ageGroup)),
                ProfileSpecification.hasMinAge(minAge),
                ProfileSpecification.hasMaxAge(maxAge),
                ProfileSpecification.hasMinGenderProbability(minGenderProbability),
                ProfileSpecification.hasMinCountryProbability(minCountryProbability)
        );

        Page<Profile> result = profileRepository.findAll(spec, pageable);

        List<ProfileDto> data = result.getContent()
                .stream()
                .map(profileMapper::toDto)
                .toList();

        PaginationLinks links = buildPaginationLinks(
                basePath,
                resolvedPage,
                resolvedLimit,
                result.getTotalPages()
        );

        return new GetAllSuccessResponse(
                "success",
                resolvedPage,
                resolvedLimit,
                result.getTotalElements(),
                result.getTotalPages(),
                links,
                data
        );
    }

    private PaginationLinks buildPaginationLinks(String basePath, int page, int limit, int totalPages) {
        String self = basePath + "?page=" + page + "&limit=" + limit;

        String next = page < totalPages
                ? basePath + "?page=" + (page + 1) + "&limit=" + limit
                : null;

        String prev = page > 1
                ? basePath + "?page=" + (page - 1) + "&limit=" + limit
                : null;

        return new PaginationLinks(self, next, prev);
    }
}
