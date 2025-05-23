package com.jandi.band_backend.univ.dto;

import com.jandi.band_backend.univ.entity.Region;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegionRespDTO {
    private Integer id;
    private String code;
    private String name;

    public RegionRespDTO(Region region) {
        this.id = region.getId();
        this.code = region.getCode();
        this.name = region.getName();
    }
}
