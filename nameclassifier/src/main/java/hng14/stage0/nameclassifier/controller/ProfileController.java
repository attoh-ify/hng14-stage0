package hng14.stage0.nameclassifier.controller;

import hng14.stage0.nameclassifier.dto.payload.CreatePayload;
import hng14.stage0.nameclassifier.dto.success.CreateSuccessResponse;
import hng14.stage0.nameclassifier.dto.success.GetAllSuccessResponse;
import hng14.stage0.nameclassifier.dto.success.GetSuccessResponse;
import hng14.stage0.nameclassifier.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/profiles")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping
    public ResponseEntity<CreateSuccessResponse> create(@Valid @RequestBody CreatePayload payload) {
        CreateSuccessResponse response = profileService.create(payload);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public GetSuccessResponse get(@PathVariable("id") String profileId) {
        return profileService.get(profileId);
    }

    @GetMapping
    public GetAllSuccessResponse getAll(
            @RequestParam(required = false) String gender,
            @RequestParam(required = false, name = "country_id") String countryId,
            @RequestParam(required = false, name = "age_group") String ageGroup,
            @RequestParam(required = false, name = "min_age") Integer minAge,
            @RequestParam(required = false, name = "max_age") Integer maxAge,
            @RequestParam(required = false, name = "min_gender_probability") Double minGenderProbability,
            @RequestParam(required = false, name = "min_country_probability") Double minCountryProbability,
            @RequestParam(required = false, name = "sort_by") String sortBy,
            @RequestParam(required = false, name = "order") String order,
            @RequestParam(defaultValue = "1", name = "page") Integer page,
            @RequestParam(defaultValue = "10", name = "limit") Integer limit
    ) {
        return profileService.getAll(
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

    @GetMapping("/search")
    public GetAllSuccessResponse searchProfiles(
            @RequestParam(name = "q") String query,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        return profileService.search(query, page, limit);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") String profileId) {
        profileService.delete(profileId);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportProfiles(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false, name = "country_id") String countryId,
            @RequestParam(required = false, name = "age_group") String ageGroup,
            @RequestParam(required = false, name = "min_age") Integer minAge,
            @RequestParam(required = false, name = "max_age") Integer maxAge,
            @RequestParam(required = false, name = "min_gender_probability") Double minGenderProbability,
            @RequestParam(required = false, name = "min_country_probability") Double minCountryProbability,
            @RequestParam(required = false, name = "sort_by") String sortBy,
            @RequestParam(required = false, name = "order") String order
    ) {
        String csv = profileService.exportProfiles(
                format,
                gender,
                countryId,
                ageGroup,
                minAge,
                maxAge,
                minGenderProbability,
                minCountryProbability,
                sortBy,
                order
        );

        String filename = "profiles_" + Instant.now().toString().replace(":", "-") + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }
}
