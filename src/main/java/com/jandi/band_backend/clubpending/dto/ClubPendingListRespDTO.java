package com.jandi.band_backend.clubpending.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ClubPendingListRespDTO {
    private Integer clubId;
    private String clubName;
    private List<ClubPendingRespDTO> pendingMembers;
    private Integer totalCount;
    private Integer pendingCount;
}
