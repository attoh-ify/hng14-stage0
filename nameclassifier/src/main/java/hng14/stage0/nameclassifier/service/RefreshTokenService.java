package hng14.stage0.nameclassifier.service;

import com.github.f4b6a3.uuid.UuidCreator;
import hng14.stage0.nameclassifier.entities.AppUser;
import hng14.stage0.nameclassifier.entities.RefreshToken;
import hng14.stage0.nameclassifier.exception.ForbiddenException;
import hng14.stage0.nameclassifier.exception.UnauthorizedException;
import hng14.stage0.nameclassifier.repositories.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshExpirationMs
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String createRefreshToken(AppUser user) {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);

        String tokenValue = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setId(UuidCreator.getTimeOrderedEpoch().toString());
        token.setToken(tokenValue);
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        token.setCreatedAt(Instant.now());
        token.setRevoked(false);

        refreshTokenRepository.save(token);

        return tokenValue;
    }

    public RefreshToken validateRefreshToken(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByTokenAndRevokedFalse(tokenValue)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new UnauthorizedException("Refresh token expired");
        }

        if (!token.getUser().isActive()) {
            throw new ForbiddenException("User account is inactive");
        }

        return token;
    }

    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    public void revokeByTokenValue(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByTokenAndRevokedFalse(tokenValue)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }
}