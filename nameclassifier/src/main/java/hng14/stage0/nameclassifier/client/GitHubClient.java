package hng14.stage0.nameclassifier.client;

import hng14.stage0.nameclassifier.config.AppProperties;
import hng14.stage0.nameclassifier.dto.response.GitHubAccessTokenResponse;
import hng14.stage0.nameclassifier.dto.response.GitHubUserResponse;
import hng14.stage0.nameclassifier.exception.UpstreamServiceException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class GitHubClient {

    private final RestClient restClient;
    private final AppProperties appProperties;

    public GitHubClient(RestClient.Builder builder, AppProperties appProperties) {
        this.restClient = builder.build();
        this.appProperties = appProperties;
    }

    public GitHubAccessTokenResponse exchangeCodeForToken(
            String code,
            String codeVerifier,
            String redirectUri,
            boolean isCli
    ) {
        AppProperties.OAuthClient oauthClient = isCli
                ? appProperties.getCli()
                : appProperties.getWeb();

        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", oauthClient.getClientId());
        form.add("client_secret", oauthClient.getClientSecret());
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("code_verifier", codeVerifier);

        try {
            GitHubAccessTokenResponse response = restClient.post()
                    .uri("https://github.com/login/oauth/access_token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(GitHubAccessTokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new UpstreamServiceException("GitHub returned an invalid response");
            }

            return response;
        } catch (Exception ex) {
            throw new UpstreamServiceException("GitHub returned an invalid response");
        }
    }

    public GitHubUserResponse fetchUser(String accessToken) {
        GitHubUserResponse response = restClient.get()
                .uri("https://api.github.com/user")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GitHubUserResponse.class);

        if (response == null || response.id() == null || response.login() == null) {
            throw new UpstreamServiceException("GitHub returned an invalid response");
        }

        return response;
    }
}