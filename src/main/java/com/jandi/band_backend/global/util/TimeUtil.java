package com.jandi.band_backend.global.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeUtil {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * UTC Instant를 KST ZonedDateTime으로 변환
     */
    public static ZonedDateTime toKST(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(KST_ZONE);
    }

    /**
     * KST ZonedDateTime을 UTC Instant로 변환
     */
    public static Instant toUTC(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return zonedDateTime.toInstant();
    }
}
