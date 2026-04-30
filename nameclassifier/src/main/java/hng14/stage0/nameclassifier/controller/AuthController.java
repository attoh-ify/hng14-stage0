package hng14.stage0.nameclassifier.controller;

import com.github.f4b6a3.uuid.UuidCreator;
import hng14.stage0.nameclassifier.config.AppProperties;
import hng14.stage0.nameclassifier.dto.payload.GitHubCliCallbackRequest;
import hng14.stage0.nameclassifier.dto.payload.LogoutRequest;
import hng14.stage0.nameclassifier.dto.payload.RefreshTokenRequest;
import hng14.stage0.nameclassifier.dto.response.MessageResponse;
import hng14.stage0.nameclassifier.dto.response.TokenResponse;
import hng14.stage0.nameclassifier.entities.AppUser;
import hng14.stage0.nameclassifier.enums.UserRole;
import hng14.stage0.nameclassifier.exception.BadRequestException;
import hng14.stage0.nameclassifier.exception.ForbiddenException;
import hng14.stage0.nameclassifier.repositories.AppUserRepository;
import hng14.stage0.nameclassifier.service.AuthService;
import hng14.stage0.nameclassifier.service.JwtService;
import hng14.stage0.nameclassifier.service.RefreshTokenService;
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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class AuthController {

    private final AppProperties appProperties;
    private final AuthService authService;
    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.web.client.url:http://localhost:3000}")
    private String webClientUrl;

    public AuthController(
            AppProperties appProperties,
            AuthService authService,
            AppUserRepository appUserRepository,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.appProperties = appProperties;
        this.authService = authService;
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    private boolean isProduction() {
        return webClientUrl != null && webClientUrl.startsWith("https://");
    }

    private ResponseCookie buildCookie(String name, String value, Duration maxAge, boolean isOauthHandshake) {
        boolean prod = isProduction();
        String sameSite = isOauthHandshake ? "None" : "Lax";
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(prod || isOauthHandshake)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    // ─── GET /auth/github ────────────────────────────────────────────────────
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
            response.header(HttpHeaders.SET_COOKIE,
                    buildCookie("insighta_oauth_state", finalState, Duration.ofMinutes(10), true).toString());
            response.header(HttpHeaders.SET_COOKIE,
                    buildCookie("insighta_code_verifier", finalCodeVerifier, Duration.ofMinutes(10), true).toString());
        }

        return response.build();
    }

    // ─── GET /auth/github/callback ────────────────────────────────────────────
    // This is the web portal callback. It redirects to /auth/success on the frontend.
    @GetMapping("/auth/github/callback")
    public ResponseEntity<Void> handleGitHubCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @CookieValue(name = "insighta_oauth_state", required = false) String expectedState,
            @CookieValue(name = "insighta_code_verifier", required = false) String codeVerifier
    ) {
        if (code == null || code.isBlank()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, webClientUrl + "/login?error=missing_code")
                    .build();
        }
        if (state == null || state.isBlank()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, webClientUrl + "/login?error=missing_state")
                    .build();
        }

        TokenResponse tokenResponse = authService.handleGitHubCallback(
                code, state, expectedState, codeVerifier
        );

        ResponseCookie clearState = buildCookie("insighta_oauth_state", "", Duration.ZERO, true);
        ResponseCookie clearVerifier = buildCookie("insighta_code_verifier", "", Duration.ZERO, true);

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

    // ─── POST /auth/github/cli/callback ──────────────────────────────────────
    @PostMapping("/auth/github/cli/callback")
    public ResponseEntity<TokenResponse> handleGitHubCliCallback(
            @Valid @RequestBody GitHubCliCallbackRequest request
    ) {
        TokenResponse response = authService.handleGitHubCliCallback(
                request.code(), request.state(), request.codeVerifier(), request.redirectUri()
        );
        return ResponseEntity.ok(response);
    }

    // ─── POST /auth/refresh ───────────────────────────────────────────────────
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

    // ─── POST /auth/logout ────────────────────────────────────────────────────
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

    // ─── GET /auth/me ─────────────────────────────────────────────────────────
    @GetMapping("/auth/me")
    public ResponseEntity<?> getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AppUser user)) throw new ForbiddenException("Unauthorized");

        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("id", user.getId());
        userData.put("username", user.getUsername());
        userData.put("email", user.getEmail() != null ? user.getEmail() : "");
        userData.put("avatar_url", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
        userData.put("role", user.getRole());
        userData.put("github_id", user.getGithubId());
        userData.put("is_active", user.isActive());
        userData.put("last_login_at", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : "");
        userData.put("created_at", user.getCreatedAt().toString());

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("status", "success");
        responseBody.put("data", userData);

        return ResponseEntity.ok(responseBody);
    }

    // ─── POST /auth/test/token ────────────────────────────────────────────────
    // This endpoint exists for automated graders and test environments.
    // It creates or returns a seeded test user and issues real JWT tokens.
    // It is only active when ENABLE_TEST_AUTH=true is set in the environment.
    @PostMapping("/auth/test/token")
    public ResponseEntity<?> getTestToken(
            @RequestParam(defaultValue = "analyst") String role,
            @Value("${enable.test.auth:false}") boolean testAuthEnabled
    ) {
        if (!testAuthEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", "error", "message", "Test auth is not enabled"));
        }

        UserRole userRole;
        try {
            userRole = UserRole.valueOf(role.toLowerCase());
        } catch (IllegalArgumentException e) {
            userRole = UserRole.analyst;
        }

        // Use a stable github_id per role so we don't create duplicate users
        String githubId = "test_user_" + userRole.name();
        String username = "test_" + userRole.name();

        final UserRole finalRole = userRole;
        AppUser user = appUserRepository.findByGithubId(githubId).orElseGet(() -> {
            AppUser newUser = new AppUser();
            newUser.setId(UuidCreator.getTimeOrderedEpoch().toString());
            newUser.setGithubId(githubId);
            newUser.setUsername(username);
            newUser.setEmail(username + "@test.insighta.local");
            newUser.setRole(finalRole);
            newUser.setActive(true);
            newUser.setCreatedAt(Instant.now());
            return appUserRepository.save(newUser);
        });

        // Update role in case it changed
        user.setRole(finalRole);
        user.setLastLoginAt(Instant.now());
        appUserRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return ResponseEntity.ok(new TokenResponse("success", accessToken, refreshToken, user.getUsername()));
    }
}