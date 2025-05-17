package com.jandi.band_backend.club.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDto<T> {
    private List<T> content;
    private Integer page;
    private Integer size;
    private Integer totalElements;
    private Integer totalPages;
    private boolean last;
}
