package hng14.stage0.nameclassifier.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubAccessTokenResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("token_type")
        String tokenType,

        String scope
) {
}