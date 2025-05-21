package com.jandi.band_backend.promo.dto;

import com.jandi.band_backend.promo.entity.Promo;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class PromoResponse {
    private Integer id;
    private Integer clubId;
    private String clubName;
    private Integer creatorId;
    private String creatorName;
    private String title;
    private BigDecimal admissionFee;
    private Instant eventDatetime;
    private String location;
    private String address;
    private String description;
    private Promo.PromoStatus status;
    private Integer viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private Instant createdAt;
    private Instant updatedAt;
    private List<String> photoUrls;

    public static PromoResponse from(Promo promo) {
        PromoResponse response = new PromoResponse();
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