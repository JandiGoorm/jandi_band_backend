package com.jandi.band_backend.promo.entity;

import com.jandi.band_backend.manage.entity.ReportReason;
import com.jandi.band_backend.user.entity.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "promo_comment_report", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"promo_comment_id", "reporter_user_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class PromoCommentReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promo_comment_report_id")
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_comment_id", nullable = false)
    private PromoComment promoComment;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    private Users reporter;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_reason_id", nullable = false)
    private ReportReason reportReason;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 
