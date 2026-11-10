package com.milaavo.auth.service;

import com.milaavo.auth.config.RefreshTokenProperties;
import com.milaavo.auth.entity.RefreshToken;
import com.milaavo.auth.exception.InvalidRefreshTokenException;
import com.milaavo.auth.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.milaavo.auth.entity.User;
import com.milaavo.auth.enums.UserStatus;
import com.milaavo.auth.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties properties;

    private final UserRepository userRepository;

    public record RotatedToken(User user, String rawToken) {
    }

    // New login = new family.
    @Transactional
    public String issue(UUID userId) {
        return createToken(userId, UUID.randomUUID());
    }

    // Commit even when we throw, so the family revoke on reuse is not rolled back.
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotatedToken rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        Instant now = Instant.now();
        RefreshToken current = refreshTokenRepository
                .findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.isRevoked()) {
            // An old token came back: someone has a copy. Kill the whole login.
            int revoked = refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            log.warn("Refresh token reuse detected. userId={} familyId={} revoked={}",
                    current.getUserId(), current.getFamilyId(), revoked);
            throw new InvalidRefreshTokenException();
        }

        if (current.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(current.getUserId()).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }

        current.setRevokedAt(now);
        String newRawToken = createToken(user.getId(), current.getFamilyId());
        return new RotatedToken(user, newRawToken);
    }

    // Logout: revoke this login's family. Unknown or empty token is fine
    // (idempotent).
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId(), Instant.now()));
    }

    private String createToken(UUID userId, UUID familyId) {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setFamilyId(familyId);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plus(properties.ttl()));
        refreshTokenRepository.save(token);

        return rawToken; // only the caller (the cookie) ever sees this
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}