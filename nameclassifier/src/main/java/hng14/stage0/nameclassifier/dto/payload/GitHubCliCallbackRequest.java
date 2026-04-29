package hng14.stage0.nameclassifier.dto.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record GitHubCliCallbackRequest(
        @NotBlank
        String code,

        @NotBlank
        String state,

        @NotBlank
        @JsonProperty("code_verifier")
        String codeVerifier,

        @NotBlank
        @JsonProperty("redirect_uri")
        String redirectUri
) {
}