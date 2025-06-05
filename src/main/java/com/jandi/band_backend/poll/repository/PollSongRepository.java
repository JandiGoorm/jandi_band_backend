package com.jandi.band_backend.poll.repository;

import com.jandi.band_backend.poll.entity.Poll;
import com.jandi.band_backend.poll.entity.PollSong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollSongRepository extends JpaRepository<PollSong, Integer> {
    List<PollSong> findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(Poll poll);
    @Query("SELECT DISTINCT ps FROM PollSong ps " +
           "LEFT JOIN FETCH ps.votes v " +
           "LEFT JOIN FETCH ps.suggester s " +
           "LEFT JOIN FETCH s.photos p " +
           "WHERE ps.poll = :poll AND ps.deletedAt IS NULL " +
           "ORDER BY ps.createdAt DESC")
    List<PollSong> findAllByPollWithVotesAndSuggester(@Param("poll") Poll poll);
}
