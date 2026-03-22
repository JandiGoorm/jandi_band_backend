package com.jandi.band_backend.poll.repository;

import com.jandi.band_backend.poll.entity.Vote;
import com.jandi.band_backend.poll.entity.Vote.VotedMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Integer> {
    Optional<Vote> findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(Integer pollSongId, Integer userId, VotedMark votedMark);
    List<Vote> findByPollSongIdAndUserIdAndDeletedAtIsNull(Integer pollSongId, Integer userId);
    List<Vote> findAllByPollSongIdAndDeletedAtIsNull(Integer pollSongId);

    @Modifying
    @Query("DELETE FROM Vote v WHERE v.user.id = :userId")
    int deleteByUserId(@Param("userId") Integer userId);
}
