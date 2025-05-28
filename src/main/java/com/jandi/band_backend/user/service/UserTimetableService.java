package com.jandi.band_backend.user.service;

import com.jandi.band_backend.global.exception.InvalidAccessException;
import com.jandi.band_backend.global.exception.TimetableNotFoundException;
import com.jandi.band_backend.user.dto.UserTimetableRespDTO;
import com.jandi.band_backend.user.dto.UserTimetableReqDTO;
import com.jandi.band_backend.user.dto.UserTimetableDetailsRespDTO;
import com.jandi.band_backend.user.entity.UserTimetable;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserTimetableRepository;
import com.jandi.band_backend.user.util.UserTimetableUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserTimetableService {
    private final UserService userService;
    private final UserTimetableRepository userTimetableRepository;
    private final UserTimetableUtil userTimetableUtil;

    /// 내 시간표 목록 조회 (userId 기반)
    @Transactional(readOnly = true)
    public List<UserTimetableRespDTO> getMyTimetables(Integer userId) {
        Users user = userService.getMyInfo(userId);

        // 내 시간표 조회 및 DTO 형태로 반환
        List<UserTimetable> myTimetables = userTimetableRepository.findByUserAndDeletedAtIsNull(user);
        return myTimetables.stream()
                .map(UserTimetableRespDTO::new).collect(Collectors.toList());
    }

    /// 특정 시간표 조회 (userId 기반)
    @Transactional(readOnly = true)
    public UserTimetableDetailsRespDTO getMyTimetableById(Integer userId, Integer timetableId) {
        UserTimetable myTimetable = getIfMyTimetable(userId, timetableId);

        return new UserTimetableDetailsRespDTO(
                myTimetable.getId(),
                myTimetable.getName(),
                userTimetableUtil.stringToJson(myTimetable.getTimetableData())
        );
    }

    /// 새 시간표 생성 (userId 기반)
    @Transactional
    public UserTimetableDetailsRespDTO createTimetable(Integer userId, UserTimetableReqDTO requestDTO) {
        Users user = userService.getMyInfo(userId);
        userTimetableUtil.validateTimetableRequest(requestDTO); // DTO 형식 검사

        // 새 테이블 생성
        UserTimetable newTimetable = new UserTimetable();
        newTimetable.setUser(user);
        newTimetable.setName(requestDTO.getName());
        newTimetable.setTimetableData(requestDTO.getTimetableData().toString());
        userTimetableRepository.save(newTimetable);

        // DTO로 반환
        return new UserTimetableDetailsRespDTO(
            newTimetable.getId(),
            newTimetable.getName(),
            userTimetableUtil.stringToJson(newTimetable.getTimetableData())
        );
    }

    /// 내 시간표 수정 (userId 기반)
    @Transactional
    public UserTimetableDetailsRespDTO updateTimetable(Integer userId, Integer timetableId, UserTimetableReqDTO requestDTO) {
        UserTimetable myTimetable = getIfMyTimetable(userId, timetableId); // 본인의 시간표일 때만 GET
        userTimetableUtil.validateTimetableRequest(requestDTO); // DTO 형식 검사

        // 시간표 수정
        myTimetable.setName(requestDTO.getName());
        myTimetable.setTimetableData(requestDTO.getTimetableData().toString());
        userTimetableRepository.save(myTimetable);

        // DTO로 반환
        return new UserTimetableDetailsRespDTO(
                myTimetable.getId(),
                myTimetable.getName(),
                userTimetableUtil.stringToJson(myTimetable.getTimetableData())
        );
    }

    /// 내 시간표 삭제 (userId 기반)
    @Transactional
    public void deleteMyTimetable(Integer userId, Integer timetableId) {
        UserTimetable myTimetable = getIfMyTimetable(userId, timetableId); // 본인의 시간표일 때만 GET

        myTimetable.setDeletedAt(LocalDateTime.now());
        userTimetableRepository.save(myTimetable);
    }

    /// 내부 메서드
    // 시간표 검색 후 본인의 시간표일때만 반환
    private UserTimetable getIfMyTimetable(Integer userId, Integer timetableId) {
        Users user = userService.getMyInfo(userId);
        UserTimetable timetable = userTimetableRepository.findByIdAndDeletedAtIsNull(timetableId)
                .orElseThrow(TimetableNotFoundException::new);

        if(!timetable.getUser().getId().equals(user.getId()))
            throw new InvalidAccessException("권한이 없습니다: 본인의 시간표가 아닙니다");
        else
            return timetable;
    }
}
