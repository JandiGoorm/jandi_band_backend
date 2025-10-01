package com.jandi.band_backend.testutil;

import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.entity.Region;
import com.jandi.band_backend.team.entity.Team;

import java.time.LocalDateTime;

/**
 * 테스트에서 사용할 공통 팩토리 클래스
 */
public class TestDataFactory {

    public static Region createTestRegion(String code, String name) {
        Region region = new Region();
        region.setCode(code);
        region.setName(name);
        return region;
    }

    public static University createTestUniversity(String name, Region region) {
        University university = new University();
        university.setName(name);
        university.setRegion(region);
        // UNIVERSITY_CODE는 7자리로 제한됨 - 타임스탬프 끝 6자리 + T 접두사
        university.setUniversityCode("T" + String.valueOf(System.currentTimeMillis()).substring(7));
        university.setAddress("테스트 주소");
        return university;
    }

    public static Users createTestUser(String kakaoId, String nickname, University university) {
        Users user = new Users();
        user.setKakaoOauthId(kakaoId);
        user.setNickname(nickname);
        user.setUniversity(university);
        user.setPosition(Users.Position.VOCAL);
        user.setIsRegistered(true);
        return user;
    }

    public static Club createTestClub(String name, University university, Users creator) {
        Club club = new Club();
        club.setName(name);
        club.setUniversity(university);
        club.setDescription("테스트 동아리 설명");
        club.setCreatedAt(LocalDateTime.now());
        club.setUpdatedAt(LocalDateTime.now());
        return club;
    }

    public static Team createTestTeam(String name, Club club, Users creator) {
        Team team = new Team();
        team.setName(name);
        team.setClub(club);
        team.setCreator(creator);
        team.setDescription("테스트 팀 설명");
        return team;
    }
}