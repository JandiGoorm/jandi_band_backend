package com.jandi.band_backend.club.entity;

import com.jandi.band_backend.poll.entity.Poll;
import com.jandi.band_backend.promo.entity.Promo;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.univ.entity.University;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "club")
@Getter
@Setter
@NoArgsConstructor
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_id")
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "chatroom_url", length = 255)
    private String chatroomUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "instagram_id", length = 50)
    private String instagramId;

    @ManyToOne
    @JoinColumn(name = "university_id", nullable = true)
    private University university;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "club")
    private List<ClubMember> clubMembers = new ArrayList<>();

    @OneToMany(mappedBy = "club")
    private List<ClubEvent> clubEvents = new ArrayList<>();

    @OneToMany(mappedBy = "club")
    private List<ClubPhoto> clubPhotos = new ArrayList<>();

    @OneToMany(mappedBy = "club")
    private List<ClubGalPhoto> clubGalPhotos = new ArrayList<>();

    @OneToMany(mappedBy = "club")
    private List<Team> teams = new ArrayList<>();

    @OneToMany(mappedBy = "club")
    private List<Poll> polls = new ArrayList<>();

    @OneToMany(mappedBy = "club")
    private List<Promo> promos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
