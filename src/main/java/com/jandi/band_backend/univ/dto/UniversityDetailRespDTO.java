package com.jandi.band_backend.univ.dto;

import com.jandi.band_backend.univ.entity.University;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UniversityDetailRespDTO {
    private String universityCode;
    private String name;
    private String region;
    private String address;

    public UniversityDetailRespDTO(University university) {
        this.universityCode = university.getUniversityCode();
        this.name = university.getName();
        this.region = university.getRegion().getName();
        this.address = university.getAddress();
    }
}
