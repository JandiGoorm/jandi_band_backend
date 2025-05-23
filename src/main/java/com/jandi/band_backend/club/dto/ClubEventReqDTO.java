// ClubEventReqDTO.java
package com.jandi.band_backend.club.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ClubEventReqDTO {
    private String name;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String location;
    private String address;
    private String description;
}