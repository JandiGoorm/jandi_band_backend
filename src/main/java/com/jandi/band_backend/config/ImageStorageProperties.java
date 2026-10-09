package com.jandi.band_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;



@Getter
@Setter
@ConfigurationProperties(prefix = "image.storage")
public class ImageStorageProperties {
    private String accessKey;
    private String secretKey;
    private String region = "auto";
    private String endpoint = "";
    private boolean pathStyle = true;
    private String bucket;
    private String publicUrl;
    private String defaultClubKey = "club-photo/rhythmeet.webp";
}
