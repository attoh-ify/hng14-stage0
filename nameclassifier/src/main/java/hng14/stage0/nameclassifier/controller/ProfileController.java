package hng14.stage0.nameclassifier.controller;

import hng14.stage0.nameclassifier.dto.payload.CreatePayload;
import hng14.stage0.nameclassifier.dto.success.CreateSuccessResponse;
import hng14.stage0.nameclassifier.dto.success.GetAllSuccessResponse;
import hng14.stage0.nameclassifier.dto.success.GetSuccessResponse;
import hng14.stage0.nameclassifier.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        if ("Profile already exists".equals(response.message())) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public GetSuccessResponse get(@PathVariable("id") String profileId) {
        return profileService.get(profileId);
    }

    @GetMapping
    public GetAllSuccessResponse getAll(
            @RequestParam(required = false) String gender,
            @RequestParam(required = false, name = "country_id") String countryId,
            @RequestParam(required = false, name = "age_group") String ageGroup
    ) {
        return profileService.getAll(gender, countryId, ageGroup);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") String profileId) {
        profileService.delete(profileId);
    }
}
