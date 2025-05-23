package com.jandi.band_backend.poll.repository;

import com.jandi.band_backend.poll.entity.Vote;
import com.jandi.band_backend.poll.entity.Vote.VotedMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Integer> {
    Optional<Vote> findByPollSongIdAndUserIdAndVotedMark(Integer pollSongId, Integer userId, VotedMark votedMark);
    List<Vote> findByPollSongIdAndUserId(Integer pollSongId, Integer userId);
}
