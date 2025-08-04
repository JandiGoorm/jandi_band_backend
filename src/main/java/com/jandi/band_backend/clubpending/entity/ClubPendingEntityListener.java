package com.jandi.band_backend.clubpending.entity;

import jakarta.persistence.PrePersist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ClubPendingEntityListener {

    @Value("${clubpending.expire.days:7}")
    private int expireDays;

    @PrePersist
    public void prePersist(ClubPending clubPending) {
        if (clubPending.getAppliedAt() == null) {
            clubPending.setAppliedAt(LocalDateTime.now());
        }

        if (clubPending.getExpiresAt() == null) {
            clubPending.setExpiresAt(LocalDateTime.now().plusDays(expireDays));
        }
    }
}
