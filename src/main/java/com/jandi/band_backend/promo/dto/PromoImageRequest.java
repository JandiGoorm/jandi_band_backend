package com.jandi.band_backend.promo.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class PromoImageRequest {
    private MultipartFile image;
} 