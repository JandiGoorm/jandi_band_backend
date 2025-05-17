package com.jandi.band_backend.club.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UniversityDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleResponse {
        private Integer id;
        private String name;
    }
}
