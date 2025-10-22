package com.jandi.band_backend.poll.entity;

import com.jandi.band_backend.user.entity.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vote", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"poll_song_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Vote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_id")
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_song_id", nullable = false)
    private PollSong pollSong;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "voted_mark", nullable = false)
    private VotedMark votedMark;
    
    @Column(name = "voted_at", nullable = false)
    private LocalDateTime votedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @PrePersist
    protected void onCreate() {
        votedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        votedAt = LocalDateTime.now();
    }

    public enum VotedMark {
        LIKE, 
        DISLIKE, 
        CANT, 
        HAJJ
    }
}
