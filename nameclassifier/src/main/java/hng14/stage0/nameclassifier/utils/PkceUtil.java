package hng14.stage0.nameclassifier.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PkceUtil {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PkceUtil() {
    }

    public static String generateState() {
        return generateSecureToken(32);
    }

    public static String generateCodeVerifier() {
        return generateSecureToken(64);
    }

    public static String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate PKCE code challenge", ex);
        }
    }

    private static String generateSecureToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}