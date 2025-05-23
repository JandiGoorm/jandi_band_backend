package com.jandi.band_backend.promo.entity;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.user.entity.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promo")
@Getter
@Setter
@NoArgsConstructor
public class Promo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promo_id")
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_user_id", nullable = false)
    private Users creator;
    
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "admission_fee", precision = 10, scale = 2)
    private BigDecimal admissionFee;
    
    @Column(name = "event_datetime")
    private LocalDateTime eventDatetime;
    
    @Column(name = "location", length = 255)
    private String location;
    
    @Column(name = "address", length = 255)
    private String address;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PromoStatus status = PromoStatus.UPCOMING;
    
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;
    
    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;
    
    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @OneToMany(mappedBy = "promo")
    private List<PromoPhoto> photos = new ArrayList<>();
    
    @OneToMany(mappedBy = "promo")
    private List<PromoLike> likes = new ArrayList<>();
    
    @OneToMany(mappedBy = "promo")
    private List<PromoReport> reports = new ArrayList<>();
    
    @OneToMany(mappedBy = "promo")
    private List<PromoComment> comments = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum PromoStatus {
        UPCOMING, ONGOING, COMPLETED
    }
} 
