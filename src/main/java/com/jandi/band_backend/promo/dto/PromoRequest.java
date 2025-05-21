package com.jandi.band_backend.promo.dto;

import com.jandi.band_backend.promo.entity.Promo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class PromoRequest {
    @NotNull(message = "클럽 ID는 필수입니다")
    private Integer clubId;

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다")
    private String title;

    private BigDecimal admissionFee;

    private Instant eventDatetime;

    @Size(max = 255, message = "장소는 255자를 초과할 수 없습니다")
    private String location;

    @Size(max = 255, message = "주소는 255자를 초과할 수 없습니다")
    private String address;

    private String description;

    private Promo.PromoStatus status;
} 