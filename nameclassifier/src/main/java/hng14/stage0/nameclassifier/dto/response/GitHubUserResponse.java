package hng14.stage0.nameclassifier.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubUserResponse(
        Long id,
        String login,
        String email,

        @JsonProperty("avatar_url")
        String avatarUrl
) {
}