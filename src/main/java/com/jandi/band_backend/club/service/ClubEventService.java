// ClubEventService.java
package com.jandi.band_backend.club.service;

import com.jandi.band_backend.club.dto.ClubEventReqDTO;
import com.jandi.band_backend.club.dto.ClubEventRespDTO;
import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubEvent;
import com.jandi.band_backend.club.repository.ClubEventRepository;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubEventService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final ClubEventRepository clubEventRepository;

    @Transactional
    public ClubEventRespDTO createClubEvent(Integer clubId, String kakaoOauthId, ClubEventReqDTO dto) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));

        Users creator = userRepository.findByKakaoOauthId(kakaoOauthId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ClubEvent clubEvent = new ClubEvent();
        clubEvent.setClub(club);
        clubEvent.setCreator(creator);
        clubEvent.setName(dto.getName());
        clubEvent.setStartDatetime(dto.getStartDatetime());
        clubEvent.setEndDatetime(dto.getEndDatetime());
        clubEvent.setLocation(dto.getLocation());
        clubEvent.setAddress(dto.getAddress());
        clubEvent.setDescription(dto.getDescription());

        ClubEvent saved = clubEventRepository.save(clubEvent);

        ClubEventRespDTO response = new ClubEventRespDTO();
        response.setId(saved.getId().longValue());
        response.setName(saved.getName());
        response.setStartDatetime(saved.getStartDatetime());
        response.setEndDatetime(saved.getEndDatetime());
        response.setLocation(saved.getLocation());
        response.setAddress(saved.getAddress());
        response.setDescription(saved.getDescription());

        return response;
    }
}