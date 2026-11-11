package com.milaavo.auth.service;

import com.milaavo.auth.config.RefreshTokenProperties;
import com.milaavo.auth.entity.RefreshToken;
import com.milaavo.auth.entity.User;
import com.milaavo.auth.enums.UserStatus;
import com.milaavo.auth.exception.InvalidRefreshTokenException;
import com.milaavo.auth.repository.RefreshTokenRepository;
import com.milaavo.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final String RAW = "raw-refresh-token";

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    private RefreshTokenService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID familyId = UUID.randomUUID();
    private RefreshToken stored;

    @BeforeEach
    void setUp() {
        // A real properties record, not a mock: it is just data.
        service = new RefreshTokenService(refreshTokenRepository,
                new RefreshTokenProperties(Duration.ofDays(7), true), userRepository);

        stored = new RefreshToken();
        stored.setUserId(userId);
        stored.setFamilyId(familyId);
        stored.setTokenHash(sha256(RAW));
        stored.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
    }

    @Test
    void issue_storesOnlyTheHash_neverTheRawToken() {
        String raw = service.issue(userId);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertEquals(sha256(raw), saved.getValue().getTokenHash());
        assertNotEquals(raw, saved.getValue().getTokenHash());
        assertEquals(userId, saved.getValue().getUserId());
    }

    @Test
    void rotate_validToken_revokesOldAndIssuesNewInSameFamily() {
        when(refreshTokenRepository.findByTokenHashForUpdate(sha256(RAW))).thenReturn(Optional.of(stored));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(UserStatus.ACTIVE)));

        RefreshTokenService.RotatedToken rotated = service.rotate(RAW);

        assertNotNull(stored.getRevokedAt(), "old token must be revoked");
        assertNotEquals(RAW, rotated.rawToken());

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertEquals(familyId, saved.getValue().getFamilyId(), "new token stays in the same family");
        assertEquals(sha256(rotated.rawToken()), saved.getValue().getTokenHash());
    }

    @Test
    void rotate_alreadyRevokedToken_isReuse_revokesWholeFamily() {
        stored.setRevokedAt(Instant.now().minusSeconds(60));
        when(refreshTokenRepository.findByTokenHashForUpdate(sha256(RAW))).thenReturn(Optional.of(stored));

        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(RAW));

        verify(refreshTokenRepository).revokeFamily(eq(familyId), any(Instant.class));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rotate_expiredToken_isRejected_andNothingIssued() {
        stored.setExpiresAt(Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByTokenHashForUpdate(sha256(RAW))).thenReturn(Optional.of(stored));

        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(RAW));

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rotate_userNoLongerActive_revokesFamily() {
        when(refreshTokenRepository.findByTokenHashForUpdate(sha256(RAW))).thenReturn(Optional.of(stored));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(UserStatus.DISABLED)));

        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(RAW));

        verify(refreshTokenRepository).revokeFamily(eq(familyId), any(Instant.class));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rotate_unknownOrMissingToken_isRejected() {
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate("not-a-real-token"));
        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(null));
    }

    @Test
    void revoke_unknownToken_isSilentNoOp() {
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.empty());

        service.revoke("not-a-real-token");

        verify(refreshTokenRepository, never()).revokeFamily(any(), any());
    }

    private User user(UserStatus status) {
        User user = new User();
        user.setId(userId);
        user.setStatus(status);
        return user;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}