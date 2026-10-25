package com.milaavo.otp.entity;

import com.milaavo.auth.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "otp_resend_attempt",
    indexes = {
        @Index(
            name = "idx_otp_resend_attempt_user_requested",
            columnList = "user_id, requested_at"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtpResendAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

}
