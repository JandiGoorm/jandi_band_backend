package com.jandi.band_backend.invite.store;

import com.jandi.band_backend.global.exception.InvalidAccessException;
import com.jandi.band_backend.persistence.ExpiringValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class InviteCodeService {
    private final ExpiringValueRepository values;
    @Value("${invite.expire.days}") private Integer expireDays;

    // id: 동아리 초대일 경우 id==club:{clubId}, 팀 초대일 경우 id==team:{teamId}
    public void saveCode(InviteType type, Integer id, String code) {
        String keyId = type + ":" + id;
        values.put(code, keyId, Duration.ofDays(expireDays));
    }

    public String getKeyId(String code) {
        String keyId = values.get(code).orElse(null);
        if(keyId == null) {
            throw new InvalidAccessException("권한 오류: code=" + code +"를 찾을 수 없습니다.");
        }
        return keyId;
    }

    public void deleteRecord(String code) {
        values.delete(code);
    }
}
