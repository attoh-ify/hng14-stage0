package hng14.stage0.nameclassifier.controller;

import hng14.stage0.nameclassifier.config.AppProperties;
import hng14.stage0.nameclassifier.dto.payload.LogoutRequest;
import hng14.stage0.nameclassifier.dto.payload.RefreshTokenRequest;
import hng14.stage0.nameclassifier.dto.response.MessageResponse;
import hng14.stage0.nameclassifier.dto.response.TokenResponse;
import hng14.stage0.nameclassifier.exception.BadRequestException;
import hng14.stage0.nameclassifier.service.AuthService;
import hng14.stage0.nameclassifier.utils.PkceUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

@RestController
public class AuthController {
    private final AppProperties appProperties;
    private final AuthService authService;

    public AuthController(AppProperties appProperties, AuthService authService) {
        this.appProperties = appProperties;
        this.authService = authService;
    }

    @GetMapping("/auth/github")
    public ResponseEntity<Void> redirectToGitHub() {
        if (appProperties.getClientId() == null || appProperties.getClientId().isBlank()) {
            throw new BadRequestException("GitHub OAuth client ID is not configured");
        }

        String state = PkceUtil.generateState();
        String codeVerifier = PkceUtil.generateCodeVerifier();
        String codeChallenge = PkceUtil.generateCodeChallenge(codeVerifier);

        URI githubAuthorizeUri = UriComponentsBuilder
                .fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", appProperties.getClientId())
                .queryParam("redirect_uri", appProperties.getRedirectUri())
                .queryParam("scope", appProperties.getScope())
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .toUri();

        ResponseCookie stateCookie = ResponseCookie.from("insighta_oauth_state", state)
                .httpOnly(true)
                .secure(false) // set true in production HTTPS
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(10))
                .build();

        ResponseCookie verifierCookie = ResponseCookie.from("insighta_code_verifier", codeVerifier)
                .httpOnly(true)
                .secure(false) // set true in production HTTPS
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(10))
                .build();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, githubAuthorizeUri.toString())
                .header(HttpHeaders.SET_COOKIE, stateCookie.toString())
                .header(HttpHeaders.SET_COOKIE, verifierCookie.toString())
                .build();
    }

    @GetMapping("/auth/github/callback")
    public ResponseEntity<TokenResponse> handleGitHubCallback(
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
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(3))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("insighta_refresh_token", response.refreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(5))
                .build();

        ResponseCookie clearStateCookie = ResponseCookie.from("insighta_oauth_state", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie clearVerifierCookie = ResponseCookie.from("insighta_code_verifier", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearStateCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearVerifierCookie.toString())
                .body(response);
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refresh(request.refreshToken());

        ResponseCookie accessCookie = ResponseCookie.from("insighta_access_token", response.accessToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(3))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("insighta_refresh_token", response.refreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
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
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie clearRefreshCookie = ResponseCookie.from("insighta_refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString())
                .body(response);
    }
}