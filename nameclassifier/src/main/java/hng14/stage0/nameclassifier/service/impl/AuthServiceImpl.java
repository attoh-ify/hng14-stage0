package hng14.stage0.nameclassifier.service.impl;

import com.github.f4b6a3.uuid.UuidCreator;
import hng14.stage0.nameclassifier.client.GitHubClient;
import hng14.stage0.nameclassifier.dto.response.GitHubAccessTokenResponse;
import hng14.stage0.nameclassifier.dto.response.GitHubUserResponse;
import hng14.stage0.nameclassifier.dto.response.MessageResponse;
import hng14.stage0.nameclassifier.dto.response.TokenResponse;
import hng14.stage0.nameclassifier.entities.AppUser;
import hng14.stage0.nameclassifier.entities.RefreshToken;
import hng14.stage0.nameclassifier.enums.UserRole;
import hng14.stage0.nameclassifier.exception.BadRequestException;
import hng14.stage0.nameclassifier.exception.ForbiddenException;
import hng14.stage0.nameclassifier.repositories.AppUserRepository;
import hng14.stage0.nameclassifier.service.AuthService;
import hng14.stage0.nameclassifier.service.JwtService;
import hng14.stage0.nameclassifier.service.RefreshTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthServiceImpl implements AuthService {
    private final GitHubClient gitHubClient;
    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthServiceImpl(
            GitHubClient gitHubClient,
            AppUserRepository appUserRepository,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.gitHubClient = gitHubClient;
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public TokenResponse handleGitHubCallback(
            String code,
            String state,
            String expectedState,
            String codeVerifier
    ) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Missing authorization code");
        }

        if (state == null || expectedState == null || !state.equals(expectedState)) {
            throw new BadRequestException("Invalid OAuth state");
        }

        if (codeVerifier == null || codeVerifier.isBlank()) {
            throw new BadRequestException("Missing PKCE verifier");
        }

        GitHubAccessTokenResponse tokenResponse = gitHubClient.exchangeCodeForToken(code, codeVerifier);
        GitHubUserResponse githubUser = gitHubClient.fetchUser(tokenResponse.accessToken());

        AppUser user = appUserRepository.findByGithubId(String.valueOf(githubUser.id()))
                .orElseGet(() -> {
                    AppUser newUser = new AppUser();
                    newUser.setId(UuidCreator.getTimeOrderedEpoch().toString());
                    newUser.setGithubId(String.valueOf(githubUser.id()));
                    newUser.setCreatedAt(Instant.now());
                    newUser.setRole(UserRole.analyst);
                    newUser.setActive(true);
                    return newUser;
                });

        user.setUsername(githubUser.login());
        user.setEmail(githubUser.email());
        user.setAvatarUrl(githubUser.avatarUrl());
        user.setLastLoginAt(Instant.now());

        AppUser savedUser = appUserRepository.save(user);

        if (!savedUser.isActive()) {
            throw new ForbiddenException("User account is inactive");
        }

        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = refreshTokenService.createRefreshToken(savedUser);

        return new TokenResponse("success", accessToken, refreshToken);
    }

    @Override
    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new BadRequestException("refresh_token is required");
        }

        RefreshToken oldToken = refreshTokenService.validateRefreshToken(refreshTokenValue);

        refreshTokenService.revoke(oldToken);

        AppUser user = oldToken.getUser();

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = refreshTokenService.createRefreshToken(user);

        return new TokenResponse("success", newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public MessageResponse logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new BadRequestException("refresh_token is required");
        }

        refreshTokenService.revokeByTokenValue(refreshTokenValue);

        return new MessageResponse("success", "Logged out successfully");
    }
}