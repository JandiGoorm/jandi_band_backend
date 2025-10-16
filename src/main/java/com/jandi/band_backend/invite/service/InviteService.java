package com.jandi.band_backend.invite.service;

import com.jandi.band_backend.global.exception.InvalidAccessException;
import com.jandi.band_backend.invite.dto.InviteLinkRespDTO;
import com.jandi.band_backend.invite.redis.InviteCodeService;
import com.jandi.band_backend.invite.redis.InviteType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class InviteService {
    private final InviteCodeService inviteCodeService;
    private final InviteUtilService inviteUtilService;
    private static final Random RANDOM = new Random();

    @Transactional
    public InviteLinkRespDTO generateInviteClubLink(Integer clubId, Integer userId) {
        inviteUtilService.isExistClub(clubId);
        if(!inviteUtilService.isMemberOfClub(clubId, userId)) {
            throw new InvalidAccessException("초대 권한이 없습니다");
        }

        // code 생성 후 Redis 서버에 저장
        String code = generateRandomCode();
        inviteCodeService.saveCode(InviteType.CLUB, clubId, code);
        return new InviteLinkRespDTO(code);
    }

    @Transactional
    public InviteLinkRespDTO generateInviteTeamLink(Integer teamId, Integer userId) {
        inviteUtilService.isExistTeam(teamId);
        if(!inviteUtilService.isMemberOfTeam(teamId, userId)) {
            throw new InvalidAccessException("초대 권한이 없습니다");
        }

        // code 생성 후 Redis 서버에 저장
        String code = generateRandomCode();
        inviteCodeService.saveCode(InviteType.TEAM, teamId, code);
        return new InviteLinkRespDTO(code);
    }

    private String generateRandomCode() {
        return RANDOM.ints('0', 'z' + 1)
                .filter(i -> Character.isAlphabetic(i) || Character.isDigit(i))
                .limit(10)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
