package com.jandi.band_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "image.storage")
public class ImageStorageProperties {
    private String accessKey;
    private String secretKey;
    private String region = "ap-northeast-2";
    private String endpoint = "";
    private boolean pathStyle;
    private String bucket;
    private String publicUrl;
    private List<String> legacyPublicUrls = List.of();
    private String defaultClubKey = "club-photo/rhythmeet.webp";
}
