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

    public AuthController(AppProperties appProperties, AuthService authService) {
        this.appProperties = appProperties;
        this.authService = authService;
    }

    @Value("${app.web.client.url}")
    private String webClientUrl;

    private String getCookieDomain() {
        if (webClientUrl != null && webClientUrl.contains("railway.app")) {
            return "up.railway.app";
        }
        return null; // Localhost uses default domain (null)
    }

    private boolean isSecure() {
        return webClientUrl != null && webClientUrl.startsWith("https");
    }

    @GetMapping("/auth/github")
    public ResponseEntity<Void> redirectToGitHub(
            @RequestParam(required = false) String client,
            @RequestParam(required = false, name = "redirect_uri") String redirectUri,
            @RequestParam(required = false) String state,
            @RequestParam(required = false, name = "code_challenge") String codeChallenge
    ) {
        boolean isCli = "cli".equalsIgnoreCase(client);

        AppProperties.OAuthClient oauthClient = isCli
                ? appProperties.getCli()
                : appProperties.getWeb();

        if (oauthClient.getClientId() == null || oauthClient.getClientId().isBlank()) {
            throw new BadRequestException("GitHub OAuth client ID is not configured");
        }

        String finalState;
        String finalCodeVerifier = null;
        String finalCodeChallenge;
        String finalRedirectUri;

        if (isCli) {
            if (redirectUri == null || redirectUri.isBlank()) {
                throw new BadRequestException("CLI redirect_uri is required");
            }

            if (state == null || state.isBlank()) {
                throw new BadRequestException("CLI state is required");
            }

            if (codeChallenge == null || codeChallenge.isBlank()) {
                throw new BadRequestException("CLI code_challenge is required");
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
                .build()
                .toUri();

        ResponseEntity.BodyBuilder response = ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, githubAuthorizeUri.toString());

        if (!isCli) {
            ResponseCookie stateCookie = ResponseCookie.from("insighta_oauth_state", finalState)
                    .httpOnly(true)
                    .secure(isSecure()) // true in production HTTPS
                    .sameSite("Lax")
                    .path("/")
                    .domain(getCookieDomain())
                    .maxAge(Duration.ofMinutes(10))
                    .build();

            ResponseCookie verifierCookie = ResponseCookie.from("insighta_code_verifier", finalCodeVerifier)
                    .httpOnly(true)
                    .secure(isSecure()) // true in production HTTPS
                    .sameSite("Lax")
                    .path("/")
                    .domain(getCookieDomain())
                    .maxAge(Duration.ofMinutes(10))
                    .build();

            response.header(HttpHeaders.SET_COOKIE, stateCookie.toString());
            response.header(HttpHeaders.SET_COOKIE, verifierCookie.toString());
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
        TokenResponse response = authService.handleGitHubCallback(
                code,
                state,
                expectedState,
                codeVerifier
        );

        ResponseCookie accessCookie = ResponseCookie.from("insighta_access_token", response.accessToken())
                .httpOnly(true)
                .secure(isSecure())
                .sameSite("Lax")
                .path("/")
                .domain(getCookieDomain())
                .maxAge(Duration.ofMinutes(3))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("insighta_refresh_token", response.refreshToken())
                .httpOnly(true)
                .secure(isSecure())
                .sameSite("Lax")
                .path("/")
                .domain(getCookieDomain())
                .maxAge(Duration.ofMinutes(5))
                .build();

        ResponseCookie clearStateCookie = ResponseCookie.from("insighta_oauth_state", "")
                .httpOnly(true)
                .secure(isSecure())
                .sameSite("Lax")
                .path("/")
                .domain(getCookieDomain())
                .maxAge(0)
                .build();

        ResponseCookie clearVerifierCookie = ResponseCookie.from("insighta_code_verifier", "")
                .httpOnly(true)
                .secure(isSecure())
                .sameSite("Lax")
                .path("/")
                .domain(getCookieDomain())
                .maxAge(0)
                .build();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearStateCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearVerifierCookie.toString())
                .header(HttpHeaders.LOCATION, webClientUrl + "/dashboard")
                .build();
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refresh(request.refreshToken());

        ResponseCookie accessCookie = ResponseCookie.from("insighta_access_token", response.accessToken())
                .httpOnly(true)
                .secure(isSecure())
                .sameSite("Lax")
                .path("/")
                .domain(getCookieDomain())
                .maxAge(Duration.ofMinutes(3))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("insighta_refresh_token", response.refreshToken())
                .httpOnly(true)
                .secure(isSecure())
                .sameSite("Lax")
                .path("/")
                .domain(getCookieDomain())
                .maxAge(Duration.ofMinutes(5))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody LogoutRequest request) {
        MessageResponse response = authService.logout(request.refreshToken());

        ResponseCookie clearAccessCookie = ResponseCookie.from("insighta_access_token", "")
                .httpOnly(true)
                .secure(isSecure())
                .sameSite("Lax")
                .path("/")
                .domain(getCookieDomain())
                .maxAge(0)
                .build();

        ResponseCookie clearRefreshCookie = ResponseCookie.from("insighta_refresh_token", "")
                .httpOnly(true)
                .secure(isSecure())
                .sameSite("Lax")
                .path("/")
                .domain(getCookieDomain())
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString())
                .body(response);
    }

    @PostMapping("/auth/github/cli/callback")
    public ResponseEntity<TokenResponse> handleGitHubCliCallback(
            @Valid @RequestBody GitHubCliCallbackRequest request
    ) {
        TokenResponse response = authService.handleGitHubCliCallback(
                request.code(),
                request.state(),
                request.codeVerifier(),
                request.redirectUri()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/auth/me")
    public ResponseEntity<?> getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!(principal instanceof AppUser user)) {
            throw new ForbiddenException("Unauthorized");
        }

        return ResponseEntity.ok(
                Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail() != null ? user.getEmail(): "",
                        "role", user.getRole(),
                        "github_id", user.getGithubId(),
                        "is_active", user.isActive(),
                        "last_login_at", user.getLastLoginAt(),
                        "created_at", user.getCreatedAt()
                )
        );
    }
}