package hng14.stage0.nameclassifier.dto.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "refresh_token is required")
        @JsonProperty("refresh_token")
        String refreshToken
) {
}