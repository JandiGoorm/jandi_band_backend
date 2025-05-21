package com.jandi.band_backend.user.entity;

import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.entity.ClubEventParticipant;
import com.jandi.band_backend.club.entity.ClubGalPhoto;
import com.jandi.band_backend.club.entity.ClubEvent;
import com.jandi.band_backend.team.entity.TeamMember;
import com.jandi.band_backend.team.entity.TeamEventParticipant;
import com.jandi.band_backend.team.entity.TeamEvent;
import com.jandi.band_backend.poll.entity.Poll;
import com.jandi.band_backend.poll.entity.PollSong;
import com.jandi.band_backend.poll.entity.Vote;
import com.jandi.band_backend.promo.entity.Promo;
import com.jandi.band_backend.promo.entity.PromoPhoto;
import com.jandi.band_backend.promo.entity.PromoLike;
import com.jandi.band_backend.promo.entity.PromoReport;
import com.jandi.band_backend.promo.entity.PromoComment;
import com.jandi.band_backend.promo.entity.PromoCommentLike;
import com.jandi.band_backend.promo.entity.PromoCommentReport;
import com.jandi.band_backend.univ.entity.University;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class Users {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer id;
    
    @Column(name = "kakao_oauth_id", nullable = false, unique = true, length = 255)
    private String kakaoOauthId;
    
    @Column(name = "nickname", nullable = false, length = 100)
    private String nickname;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "position")
    private Position position;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id")
    private University university;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "admin_role", nullable = false)
    private AdminRole adminRole = AdminRole.USER;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "deleted_at")
    private Instant deletedAt;
    
    @OneToMany(mappedBy = "user")
    private List<UserPhoto> photos = new ArrayList<>();
    
    @OneToMany(mappedBy = "user")
    private List<UserTimetable> timetables = new ArrayList<>();
    
    @OneToMany(mappedBy = "user")
    private List<ClubMember> clubMemberships = new ArrayList<>();
    
    @OneToMany(mappedBy = "user")
    private List<ClubEventParticipant> clubEventParticipations = new ArrayList<>();
    
    @OneToMany(mappedBy = "uploader")
    private List<ClubGalPhoto> uploadedClubGalPhotos = new ArrayList<>();
    
    @OneToMany(mappedBy = "creator")
    private List<ClubEvent> createdClubEvents = new ArrayList<>();
    
    @OneToMany(mappedBy = "user")
    private List<TeamMember> teamMemberships = new ArrayList<>();
    
    @OneToMany(mappedBy = "user")
    private List<TeamEventParticipant> teamEventParticipations = new ArrayList<>();
    
    @OneToMany(mappedBy = "creator")
    private List<TeamEvent> createdTeamEvents = new ArrayList<>();
    
    @OneToMany(mappedBy = "creator")
    private List<Poll> createdPolls = new ArrayList<>();
    
    @OneToMany(mappedBy = "suggester")
    private List<PollSong> suggestedPollSongs = new ArrayList<>();
    
    @OneToMany(mappedBy = "user")
    private List<Vote> votes = new ArrayList<>();
    
    @OneToMany(mappedBy = "creator")
    private List<Promo> createdPromos = new ArrayList<>();
    
    @OneToMany(mappedBy = "uploader")
    private List<PromoPhoto> uploadedPromoPhotos = new ArrayList<>();
    
    @OneToMany(mappedBy = "user")
    private List<PromoLike> promoLikes = new ArrayList<>();
    
    @OneToMany(mappedBy = "reporter")
    private List<PromoReport> promoReports = new ArrayList<>();
    
    @OneToMany(mappedBy = "creator")
    private List<PromoComment> createdPromoComments = new ArrayList<>();
    
    @OneToMany(mappedBy = "user")
    private List<PromoCommentLike> promoCommentLikes = new ArrayList<>();
    
    @OneToMany(mappedBy = "reporter")
    private List<PromoCommentReport> promoCommentReports = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        position = null;
        university = null;

        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    public enum Position {
        VOCAL, GUITAR, KEYBOARD, BASS, DRUM, OTHER;

        public static Position from(String name) {
            try {
                return Position.valueOf(name);
            } catch (Exception e) {
                return null;
            }
        }
    }
    
    public enum AdminRole {
        ADMIN, USER
    }
} 