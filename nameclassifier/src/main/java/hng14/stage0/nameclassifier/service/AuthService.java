package hng14.stage0.nameclassifier.service;

import hng14.stage0.nameclassifier.dto.response.MessageResponse;
import hng14.stage0.nameclassifier.dto.response.TokenResponse;

public interface AuthService {
    TokenResponse handleGitHubCallback(String code, String state, String expectedState, String codeVerifier);
    TokenResponse refresh(String refreshToken);
    MessageResponse logout(String refreshToken);
    TokenResponse handleGitHubCliCallback(String code, String state, String codeVerifier, String redirectUri);
}