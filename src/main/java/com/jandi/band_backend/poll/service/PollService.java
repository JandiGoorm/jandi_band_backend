package com.jandi.band_backend.poll.service;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.poll.dto.*;
import com.jandi.band_backend.poll.entity.Poll;
import com.jandi.band_backend.poll.entity.PollSong;
import com.jandi.band_backend.poll.repository.PollRepository;
import com.jandi.band_backend.poll.repository.PollSongRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final PollSongRepository pollSongRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;

    @Transactional
    public PollRespDTO createPoll(PollCreateReqDTO requestDto, Integer currentUserId) {
        // 동아리 조회
        Club club = clubRepository.findById(requestDto.getClubId())
                .orElseThrow(() -> new IllegalArgumentException("해당 동아리를 찾을 수 없습니다."));

        // 사용자 조회
        Users creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 투표 생성
        Poll poll = new Poll();
        poll.setClub(club);
        poll.setTitle(requestDto.getTitle());
        poll.setStartDatetime(Instant.now()); // UTC로 현재 시간 저장
        poll.setEndDatetime(requestDto.getEndDatetime()); // 클라이언트에서 전달한 UTC 시간 그대로 저장
        poll.setCreator(creator);

        Poll savedPoll = pollRepository.save(poll);

        return PollRespDTO.fromEntity(savedPoll); // fromEntity 내에서 KST로 변환됨
    }

    @Transactional(readOnly = true)
    public Page<PollRespDTO> getPollsByClub(Integer clubId, Pageable pageable) {
        // 동아리 조회
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("해당 동아리를 찾을 수 없습니다."));

        // 동아리에 해당하는 투표 조회
        Page<Poll> polls = pollRepository.findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(club, pageable);

        return polls.map(PollRespDTO::fromEntity); // fromEntity 내에서 KST로 변환됨
    }

    @Transactional(readOnly = true)
    public PollDetailRespDTO getPollDetail(Integer pollId) {
        // 투표 조회
        Poll poll = pollRepository.findByIdAndDeletedAtIsNull(pollId)
                .orElseThrow(() -> new IllegalArgumentException("해당 투표를 찾을 수 없습니다."));

        // 투표에 해당하는 노래 목록 조회
        List<PollSong> pollSongs = pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(poll);

        // PollSongResponseDto로 변환
        List<PollSongRespDTO> songResponseDtos = pollSongs.stream()
                .map(PollSongRespDTO::fromEntity) // fromEntity 내에서 KST로 변환됨
                .collect(Collectors.toList());

        return PollDetailRespDTO.fromEntity(poll, songResponseDtos); // fromEntity 내에서 KST로 변환됨
    }

    @Transactional
    public PollSongRespDTO addSongToPoll(Integer pollId, PollSongCreateReqDTO requestDto, Integer currentUserId) {
        // 투표 조회
        Poll poll = pollRepository.findByIdAndDeletedAtIsNull(pollId)
                .orElseThrow(() -> new IllegalArgumentException("해당 투표를 찾을 수 없습니다."));

        // 사용자 조회
        Users suggester = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 노래 추가
        PollSong pollSong = new PollSong();
        pollSong.setPoll(poll);
        pollSong.setSongName(requestDto.getSongName());
        pollSong.setArtistName(requestDto.getArtistName());
        pollSong.setYoutubeUrl(requestDto.getYoutubeUrl());
        pollSong.setDescription(requestDto.getDescription());
        pollSong.setSuggester(suggester);

        PollSong savedPollSong = pollSongRepository.save(pollSong);

        return PollSongRespDTO.fromEntity(savedPollSong); // fromEntity 내에서 KST로 변환됨
    }
}
