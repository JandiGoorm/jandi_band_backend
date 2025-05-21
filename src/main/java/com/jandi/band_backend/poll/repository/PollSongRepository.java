package com.jandi.band_backend.poll.repository;

import com.jandi.band_backend.poll.entity.Poll;
import com.jandi.band_backend.poll.entity.PollSong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollSongRepository extends JpaRepository<PollSong, Integer> {
    List<PollSong> findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(Poll poll);
}
