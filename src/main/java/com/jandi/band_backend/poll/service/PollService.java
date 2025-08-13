package com.jandi.band_backend.poll.service;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.global.exception.*;
import com.jandi.band_backend.global.util.EntityValidationUtil;
import com.jandi.band_backend.global.util.UserValidationUtil;
import com.jandi.band_backend.poll.dto.*;
import com.jandi.band_backend.poll.entity.Poll;
import com.jandi.band_backend.poll.entity.PollSong;
import com.jandi.band_backend.poll.entity.Vote;
import com.jandi.band_backend.poll.entity.Vote.VotedMark;
import com.jandi.band_backend.poll.repository.PollRepository;
import com.jandi.band_backend.poll.repository.PollSongRepository;
import com.jandi.band_backend.poll.repository.VoteRepository;
import com.jandi.band_backend.user.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final PollSongRepository pollSongRepository;
    private final VoteRepository voteRepository;
    private final EntityValidationUtil entityValidationUtil;
    private final UserValidationUtil userValidationUtil;

    @Transactional
    public PollRespDTO createPoll(PollReqDTO requestDto, Integer currentUserId) {
        Club club = entityValidationUtil.validateClubExists(requestDto.getClubId());

        Users creator = userValidationUtil.getUserById(currentUserId);

        Poll poll = new Poll();
        poll.setClub(club);
        poll.setTitle(requestDto.getTitle());
        poll.setStartDatetime(LocalDateTime.now());
        poll.setEndDatetime(requestDto.getEndDatetime());
        poll.setCreator(creator);

        Poll savedPoll = pollRepository.save(poll);

        return convertToPollRespDTO(savedPoll);
    }

    @Transactional(readOnly = true)
    public Page<PollRespDTO> getPollsByClub(Integer clubId, Pageable pageable) {
        Club club = entityValidationUtil.validateClubExists(clubId);

        Page<Poll> polls = pollRepository.findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(club, pageable);

        return polls.map(this::convertToPollRespDTO);
    }

    @Transactional(readOnly = true)
    public PollDetailRespDTO getPollDetail(Integer pollId, Integer currentUserId) {
        Poll poll = entityValidationUtil.validatePollExists(pollId);

        List<PollSong> pollSongs = pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(poll);

        List<PollSongRespDTO> songResponseDtos = pollSongs.stream()
                .map(pollSong -> convertToPollSongRespDTO(pollSong, currentUserId))
                .collect(Collectors.toList());

        return convertToPollDetailRespDTO(poll, songResponseDtos);
    }

    @Transactional(readOnly = true)
    public List<PollSongResultRespDTO> getPollSongs(Integer pollId, String sortBy, String order, Integer currentUserId) {
        Poll poll = entityValidationUtil.validatePollExists(pollId);

        List<PollSong> pollSongs = pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(poll);

        List<PollSongResultRespDTO> songResultDtos = pollSongs.stream()
                .map(this::convertToPollSongResultRespDTO)
                .collect(Collectors.toList());

        return applySortingForResult(songResultDtos, sortBy, order);
    }

    @Transactional
    public PollSongRespDTO addSongToPoll(Integer pollId, PollSongReqDTO requestDto, Integer currentUserId) {
        Poll poll = entityValidationUtil.validatePollExists(pollId);

        Users suggester = userValidationUtil.getUserById(currentUserId);

        PollSong pollSong = new PollSong();
        pollSong.setPoll(poll);
        pollSong.setSongName(requestDto.getSongName());
        pollSong.setArtistName(requestDto.getArtistName());
        pollSong.setYoutubeUrl(requestDto.getYoutubeUrl());
        pollSong.setDescription(requestDto.getDescription());
        pollSong.setSuggester(suggester);

        PollSong savedPollSong = pollSongRepository.save(pollSong);

        return convertToPollSongRespDTO(savedPollSong);
    }

    @Transactional
    public PollSongRespDTO setVoteForSong(Integer pollId, Integer songId, String voteType, Integer currentUserId) {
        Users user = userValidationUtil.getUserById(currentUserId);

        PollSong pollSong = entityValidationUtil.validatePollSongBelongsToPoll(pollId, songId);

        VotedMark votedMark = convertToVotedMark(voteType);

        Optional<Vote> existingVote = voteRepository.findByPollSongIdAndUserId(songId, currentUserId)
                .stream().findFirst();    // 1인 1투표 (유니크 제약조건)

        if (existingVote.isPresent()) {
            Vote vote = existingVote.get();    // Managed Entity 이므로 더티체킹 O (save 불필요)

            if (vote.getVotedMark() == votedMark) {
                throw new VoteAlreadyExistsException(
                    "이미 이 노래에 대한 '" + voteType + "' 투표가 존재합니다. " +
                    "취소하려면 DELETE 요청을 사용하세요."
                );
            }

            vote.setVotedMark(votedMark);
        } else {
            Vote vote = new Vote();    // Transient Entity 이므로 더티체킹 X (save 필요)
            vote.setPollSong(pollSong);
            vote.setUser(user);
            vote.setVotedMark(votedMark);
            voteRepository.save(vote);
        }

        return convertToPollSongRespDTO(pollSong, currentUserId);
    }

    @Transactional
    public PollSongRespDTO removeVoteFromSong(Integer pollId, Integer songId, String voteType, Integer currentUserId) {
        PollSong pollSong = entityValidationUtil.validatePollSongBelongsToPoll(pollId, songId);

        VotedMark votedMark = convertToVotedMark(voteType);

        Vote vote = voteRepository.findByPollSongIdAndUserIdAndVotedMark(
                songId,
                currentUserId,
                votedMark
        ).orElseThrow(() -> new VoteNotFoundException(
                "사용자의 해당 노래에 대한 " + voteType + " 타입의 투표를 찾을 수 없습니다."
        ));

        voteRepository.delete(vote);

        int likeCount = calculateVoteCount(pollSong, "LIKE");
        int dislikeCount = calculateVoteCount(pollSong, "DISLIKE");
        int cantCount = calculateVoteCount(pollSong, "CANT");
        int hajjCount = calculateVoteCount(pollSong, "HAJJ");

        String suggesterProfilePhoto = pollSong.getSuggester().getPhotos().stream()
                .filter(photo -> photo.getIsCurrent() && photo.getDeletedAt() == null)
                .map(photo -> photo.getImageUrl())
                .findFirst()
                .orElse(null);

        return PollSongRespDTO.builder()
                .id(pollSong.getId())
                .pollId(pollSong.getPoll().getId())
                .songName(pollSong.getSongName())
                .artistName(pollSong.getArtistName())
                .youtubeUrl(pollSong.getYoutubeUrl())
                .description(pollSong.getDescription())
                .suggesterId(pollSong.getSuggester().getId())
                .suggesterName(pollSong.getSuggester().getNickname())
                .suggesterProfilePhoto(suggesterProfilePhoto)
                .createdAt(pollSong.getCreatedAt())
                .likeCount(likeCount)
                .dislikeCount(dislikeCount)
                .cantCount(cantCount)
                .hajjCount(hajjCount)
                .userVoteType(null)
                .build();
    }

    private PollRespDTO convertToPollRespDTO(Poll poll) {
        return PollRespDTO.builder()
                .id(poll.getId())
                .title(poll.getTitle())
                .clubId(
                        poll.getClub() != null ?
                        poll.getClub().getId() :
                        null
                )
                .clubName(
                        poll.getClub() != null ?
                        poll.getClub().getName() :
                        null
                )
                .startDatetime(poll.getStartDatetime())
                .endDatetime(poll.getEndDatetime())
                .creatorId(
                        poll.getCreator() != null ?
                        poll.getCreator().getId() :
                        null
                )
                .creatorName(
                        poll.getCreator() != null ?
                        poll.getCreator().getNickname() :
                        null
                )
                .createdAt(poll.getCreatedAt())
                .build();
    }

    private PollDetailRespDTO convertToPollDetailRespDTO(Poll poll, List<PollSongRespDTO> songs) {
        return PollDetailRespDTO.builder()
                .id(poll.getId())
                .title(poll.getTitle())
                .clubId(
                        poll.getClub() != null ?
                        poll.getClub().getId() :
                        null
                )
                .clubName(
                        poll.getClub() != null ?
                        poll.getClub().getName() :
                        null
                )
                .startDatetime(poll.getStartDatetime())
                .endDatetime(poll.getEndDatetime())
                .creatorId(
                        poll.getCreator() != null ?
                        poll.getCreator().getId() :
                        null
                )
                .creatorName(
                        poll.getCreator() != null ?
                        poll.getCreator().getNickname() :
                        null
                )
                .createdAt(poll.getCreatedAt())
                .songs(songs)
                .build();
    }

    private PollSongRespDTO convertToPollSongRespDTO(PollSong pollSong) {
        return convertToPollSongRespDTO(pollSong, null);
    }

    private PollSongRespDTO convertToPollSongRespDTO(PollSong pollSong, Integer currentUserId) {
        String userVoteType = null;
        if (currentUserId != null) {
            Optional<Vote> userVote = pollSong.getVotes().stream()
                    .filter(vote -> vote.getUser().getId().equals(currentUserId))
                    .findFirst();

            if (userVote.isPresent()) {
                userVoteType = userVote.get().getVotedMark().name();
            }
        }

        String suggesterProfilePhoto = pollSong.getSuggester().getPhotos().stream()
                .filter(photo -> photo.getIsCurrent() && photo.getDeletedAt() == null)
                .map(photo -> photo.getImageUrl())
                .findFirst()
                .orElse(null);

        return PollSongRespDTO.builder()
                .id(pollSong.getId())
                .pollId(
                        pollSong.getPoll() != null ?
                        pollSong.getPoll().getId() :
                        null
                )
                .songName(pollSong.getSongName())
                .artistName(pollSong.getArtistName())
                .youtubeUrl(pollSong.getYoutubeUrl())
                .description(pollSong.getDescription())
                .suggesterId(pollSong.getSuggester().getId())
                .suggesterName(pollSong.getSuggester().getNickname())
                .suggesterProfilePhoto(suggesterProfilePhoto)
                .createdAt(pollSong.getCreatedAt())
                .likeCount(calculateVoteCount(pollSong, "LIKE"))
                .dislikeCount(calculateVoteCount(pollSong, "DISLIKE"))
                .cantCount(calculateVoteCount(pollSong, "CANT"))
                .hajjCount(calculateVoteCount(pollSong, "HAJJ"))
                .userVoteType(userVoteType)
                .build();
    }

    private PollSongResultRespDTO convertToPollSongResultRespDTO(PollSong pollSong) {
        return PollSongResultRespDTO.builder()
                .id(pollSong.getId())
                .pollId(pollSong.getPoll() != null ? pollSong.getPoll().getId() : null)
                .songName(pollSong.getSongName())
                .artistName(pollSong.getArtistName())
                .createdAt(pollSong.getCreatedAt())
                .likeCount(calculateVoteCount(pollSong, "LIKE"))
                .dislikeCount(calculateVoteCount(pollSong, "DISLIKE"))
                .cantCount(calculateVoteCount(pollSong, "CANT"))
                .hajjCount(calculateVoteCount(pollSong, "HAJJ"))
                .build();
    }

    private int calculateVoteCount(PollSong pollSong, String voteMark) {
        return (int) pollSong.getVotes().stream()
                .filter(vote -> vote.getVotedMark().name().equals(voteMark))
                .count();
    }

    private VotedMark convertToVotedMark(String voteType) {
        if (voteType == null || voteType.trim().isEmpty()) {
            throw new BadRequestException("투표 타입이 null이거나 비어있습니다.");
        }

        return switch (voteType.toUpperCase()) {
            case "LIKE", "좋아요" -> VotedMark.LIKE;
            case "DISLIKE", "별로에요" -> VotedMark.DISLIKE;
            case "CANT", "실력부족" -> VotedMark.CANT;
            case "HAJJ", "하고싶지_않은데_존중해요" -> VotedMark.HAJJ;
            default -> throw new BadRequestException("유효하지 않은 투표 타입입니다: " + voteType);
        };
    }

    private List<PollSongResultRespDTO> applySortingForResult(List<PollSongResultRespDTO> songs, String sortBy, String order) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            throw new BadRequestException("정렬 기준이 null이거나 비어있습니다.");
        }

        Comparator<PollSongResultRespDTO> comparator;

        switch (sortBy.toUpperCase()) {
            case "LIKE":
                comparator = Comparator.comparingInt(PollSongResultRespDTO::getLikeCount);
                break;
            case "DISLIKE":
                comparator = Comparator.comparingInt(PollSongResultRespDTO::getDislikeCount);
                break;
            case "SCORE":
                comparator = Comparator.comparingInt(this::calculateScore);
                break;
            default:
                throw new BadRequestException("유효하지 않은 정렬 기준입니다: " + sortBy);
        }

        // 내림차순이 기본값
        if ("asc".equalsIgnoreCase(order)) {
            return songs.stream()
                    .sorted(comparator)
                    .collect(Collectors.toList());
        } else {
            return songs.stream()
                    .sorted(comparator.reversed())
                    .collect(Collectors.toList());
        }
    }

    // 점수 = (긍정 투표 수: LIKE + HAJJ) - (부정 투표 수: DISLIKE + CANT)
    private int calculateScore(PollSongResultRespDTO song) {
        int positiveVotes = song.getLikeCount() + song.getHajjCount();
        int negativeVotes = song.getDislikeCount() + song.getCantCount();
        return positiveVotes - negativeVotes;
    }
}
