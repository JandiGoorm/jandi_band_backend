package com.jandi.band_backend.univ.dto;

import com.jandi.band_backend.univ.entity.University;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversityRespDTO {
    private Integer id;
    private String name;

    public UniversityRespDTO(University university) {
        this.id = university.getId();
        this.name = university.getName();
    }
}
