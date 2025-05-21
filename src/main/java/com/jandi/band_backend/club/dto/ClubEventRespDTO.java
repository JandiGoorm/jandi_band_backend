// ClubEventRespDTO.java
package com.jandi.band_backend.club.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ClubEventRespDTO {
    private Long id;
    private String name;
    private Instant startDatetime;
    private Instant endDatetime;
    private String location;
    private String address;
    private String description;
}