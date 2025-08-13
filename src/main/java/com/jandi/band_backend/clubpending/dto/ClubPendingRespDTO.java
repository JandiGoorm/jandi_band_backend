package com.jandi.band_backend.clubpending.dto;

import com.jandi.band_backend.clubpending.entity.ClubPending;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ClubPendingRespDTO {
    private Integer pendingId;
    private Integer clubId;
    private String clubName;
    private Integer userId;
    private String userNickname;
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime processedAt;
    private LocalDateTime expiresAt;
    private Integer processedBy;
    private String processedByNickname;

    public static ClubPendingRespDTO from(ClubPending pending) {
        return ClubPendingRespDTO.builder()
                .pendingId(pending.getId())
                .clubId(pending.getClub().getId())
                .clubName(pending.getClub().getName())
                .userId(pending.getUser().getId())
                .userNickname(pending.getUser().getNickname())
                .status(pending.getStatus().name())
                .appliedAt(pending.getAppliedAt())
                .processedAt(pending.getProcessedAt())
                .expiresAt(pending.getExpiresAt())
                .processedBy(pending.getProcessedBy() != null ? pending.getProcessedBy().getId() : null)
                .processedByNickname(pending.getProcessedBy() != null ? pending.getProcessedBy().getNickname() : null)
                .build();
    }
}
