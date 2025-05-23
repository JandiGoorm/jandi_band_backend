package com.jandi.band_backend.univ.enums;

import lombok.Getter;

@Getter
public enum UnivType {
    COLLEGE("전문대학"),
    UNIVERSITY("대학교"),
    GRADUATE("일반대학원"),
    POLYTECH("한국폴리텍");

    private final String keyword;

    UnivType(String keyword) {
        this.keyword = keyword;
    }

    public static UnivType from(String type) {
        try {
            return UnivType.valueOf(type.toUpperCase());
        }catch (Exception e) {
            throw new IllegalArgumentException("잘못된 type: " + type);
        }
    }
}