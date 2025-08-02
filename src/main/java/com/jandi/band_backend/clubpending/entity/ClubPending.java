package com.jandi.band_backend.clubpending.entity;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.user.entity.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "club_pending", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"club_id", "user_id", "status"})
})
@Getter
@Setter
@NoArgsConstructor
public class ClubPending {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_pending_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PendingStatus status = PendingStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "processed_by")
    private Users processedBy; // 승인/거부한 관리자

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public enum PendingStatus {
        PENDING,
        APPROVED,
        REJECTED,
        EXPIRED
    }

    @PrePersist
    protected void onCreate() {
        appliedAt = LocalDateTime.now();
        expiresAt = LocalDateTime.now().plusDays(7);
    }
}
