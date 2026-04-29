package hng14.stage0.nameclassifier.controller;

import hng14.stage0.nameclassifier.config.AppProperties;
import hng14.stage0.nameclassifier.dto.payload.GitHubCliCallbackRequest;
import hng14.stage0.nameclassifier.dto.payload.LogoutRequest;
import hng14.stage0.nameclassifier.dto.payload.RefreshTokenRequest;
import hng14.stage0.nameclassifier.dto.response.MessageResponse;
import hng14.stage0.nameclassifier.dto.response.TokenResponse;
import hng14.stage0.nameclassifier.entities.AppUser;
import hng14.stage0.nameclassifier.exception.BadRequestException;
import hng14.stage0.nameclassifier.exception.ForbiddenException;
import hng14.stage0.nameclassifier.service.AuthService;
import hng14.stage0.nameclassifier.utils.PkceUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@RestController
public class AuthController {

    private final AppProperties appProperties;
    private final AuthService authService;

    @Value("${app.web.client.url}")
    private String webClientUrl;

    public AuthController(AppProperties appProperties, AuthService authService) {
        this.appProperties = appProperties;
        this.authService = authService;
    }

    private boolean isProduction() {
        return webClientUrl != null && (
                webClientUrl.contains("railway.app") ||
                        webClientUrl.startsWith("https://")
        );
    }

    private ResponseCookie buildCookie(String name, String value, Duration maxAge, boolean isOauthHandshake) {
        // OAuth handshake cookies (state, verifier) need SameSite=None so the browser
        // sends them back when GitHub redirects to the backend callback.
        // Auth tokens use SameSite=Lax which is safe for normal navigation.
        String sameSite = isOauthHandshake ? "None" : "Lax";
        boolean secure = isProduction();

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge);

        return builder.build();
    }

    @GetMapping("/auth/github")
    public ResponseEntity<Void> redirectToGitHub(
            @RequestParam(required = false) String client,
            @RequestParam(required = false, name = "redirect_uri") String redirectUri,
            @RequestParam(required = false) String state,
            @RequestParam(required = false, name = "code_challenge") String codeChallenge
    ) {
        boolean isCli = "cli".equalsIgnoreCase(client);
        AppProperties.OAuthClient oauthClient = isCli ? appProperties.getCli() : appProperties.getWeb();

        if (oauthClient.getClientId() == null || oauthClient.getClientId().isBlank()) {
            throw new BadRequestException("GitHub OAuth client ID is not configured");
        }

        String finalState, finalCodeVerifier = null, finalCodeChallenge, finalRedirectUri;

        if (isCli) {
            if (redirectUri == null || state == null || codeChallenge == null) {
                throw new BadRequestException("CLI request missing required PKCE parameters");
            }
            finalState = state;
            finalCodeChallenge = codeChallenge;
            finalRedirectUri = redirectUri;
        } else {
            finalState = PkceUtil.generateState();
            finalCodeVerifier = PkceUtil.generateCodeVerifier();
            finalCodeChallenge = PkceUtil.generateCodeChallenge(finalCodeVerifier);
            finalRedirectUri = oauthClient.getRedirectUri();
        }

        URI githubAuthorizeUri = UriComponentsBuilder
                .fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", oauthClient.getClientId())
                .queryParam("redirect_uri", finalRedirectUri)
                .queryParam("scope", appProperties.getScope())
                .queryParam("state", finalState)
                .queryParam("code_challenge", finalCodeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build().toUri();

        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, githubAuthorizeUri.toString());

        if (!isCli) {
            // These cookies travel with the user through GitHub and back,
            // so they MUST be SameSite=None (cross-site redirect).
            response.header(HttpHeaders.SET_COOKIE,
                    buildCookie("insighta_oauth_state", finalState, Duration.ofMinutes(10), true).toString());
            response.header(HttpHeaders.SET_COOKIE,
                    buildCookie("insighta_code_verifier", finalCodeVerifier, Duration.ofMinutes(10), true).toString());
        }

        return response.build();
    }

    @GetMapping("/auth/github/callback")
    public ResponseEntity<Void> handleGitHubCallback(
            @RequestParam String code,
            @RequestParam String state,
            @CookieValue(name = "insighta_oauth_state", required = false) String expectedState,
            @CookieValue(name = "insighta_code_verifier", required = false) String codeVerifier
    ) {
        TokenResponse tokenResponse = authService.handleGitHubCallback(
                code, state, expectedState, codeVerifier
        );

        // Clear the OAuth handshake cookies
        ResponseCookie clearState = buildCookie("insighta_oauth_state", "", Duration.ZERO, true);
        ResponseCookie clearVerifier = buildCookie("insighta_code_verifier", "", Duration.ZERO, true);

        // Redirect to /auth/success on the Next.js portal with tokens in the URL.
        // Next.js will call /api/auth/set-tokens to store them as httpOnly cookies,
        // then redirect to /dashboard. This is necessary because the backend and
        // web portal are on different domains in production, so the backend cannot
        // directly set cookies on the web portal's domain.
        String successUrl = UriComponentsBuilder
                .fromUriString(webClientUrl + "/auth/success")
                .queryParam("access_token", tokenResponse.accessToken())
                .queryParam("refresh_token", tokenResponse.refreshToken())
                .queryParam("username", tokenResponse.username())
                .build()
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, clearState.toString())
                .header(HttpHeaders.SET_COOKIE, clearVerifier.toString())
                .header(HttpHeaders.LOCATION, successUrl)
                .build();
    }

    @PostMapping("/auth/github/cli/callback")
    public ResponseEntity<TokenResponse> handleGitHubCliCallback(
            @Valid @RequestBody GitHubCliCallbackRequest request
    ) {
        return ResponseEntity.ok(authService.handleGitHubCliCallback(
                request.code(), request.state(), request.codeVerifier(), request.redirectUri()
        ));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refresh(request.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        buildCookie("insighta_access_token", response.accessToken(), Duration.ofMinutes(3), false).toString())
                .header(HttpHeaders.SET_COOKIE,
                        buildCookie("insighta_refresh_token", response.refreshToken(), Duration.ofMinutes(5), false).toString())
                .body(response);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody LogoutRequest request) {
        MessageResponse response = authService.logout(request.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        buildCookie("insighta_access_token", "", Duration.ZERO, false).toString())
                .header(HttpHeaders.SET_COOKIE,
                        buildCookie("insighta_refresh_token", "", Duration.ZERO, false).toString())
                .body(response);
    }

    @GetMapping("/auth/me")
    public ResponseEntity<?> getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AppUser user)) throw new ForbiddenException("Unauthorized");

        // Return { status, data: { ...user } } shape expected by the web portal and CLI
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail() != null ? user.getEmail() : "",
                        "avatar_url", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                        "role", user.getRole(),
                        "github_id", user.getGithubId(),
                        "is_active", user.isActive(),
                        "last_login_at", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : "",
                        "created_at", user.getCreatedAt().toString()
                )
        ));
    }
}