package com.jandi.band_backend.club.entity;

import com.jandi.band_backend.user.entity.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "club_event_participant", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"club_event_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ClubEventParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_event_participant_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "club_event_id", nullable = false)
    private ClubEvent clubEvent;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
