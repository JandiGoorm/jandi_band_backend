package com.jandi.band_backend.promo.dto;

import com.jandi.band_backend.promo.entity.Promo;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class PromoRespDTO {
    private Integer id;
    private Integer clubId;
    private String clubName;
    private Integer creatorId;
    private String creatorName;
    private String title;
    private BigDecimal admissionFee;
    private LocalDateTime eventDatetime;
    private String location;
    private String address;
    private String description;
    private Promo.PromoStatus status;
    private Integer viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> photoUrls;

    public static PromoRespDTO from(Promo promo) {
        PromoRespDTO response = new PromoRespDTO();
        response.setId(promo.getId());
        response.setClubId(promo.getClub().getId());
        response.setClubName(promo.getClub().getName());
        response.setCreatorId(promo.getCreator().getId());
        response.setCreatorName(promo.getCreator().getNickname());
        response.setTitle(promo.getTitle());
        response.setAdmissionFee(promo.getAdmissionFee());
        response.setEventDatetime(promo.getEventDatetime());
        response.setLocation(promo.getLocation());
        response.setAddress(promo.getAddress());
        response.setDescription(promo.getDescription());
        response.setStatus(promo.getStatus());
        response.setViewCount(promo.getViewCount());
        response.setCommentCount(promo.getCommentCount());
        response.setLikeCount(promo.getLikeCount());
        response.setCreatedAt(promo.getCreatedAt());
        response.setUpdatedAt(promo.getUpdatedAt());
        response.setPhotoUrls(promo.getPhotos().stream()
                .map(photo -> photo.getImageUrl())
                .collect(Collectors.toList()));
        return response;
    }
} 