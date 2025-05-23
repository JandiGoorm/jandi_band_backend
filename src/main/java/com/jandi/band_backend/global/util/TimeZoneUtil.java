package com.jandi.band_backend.global.util;

import java.time.ZoneId;

public class TimeZoneUtil {

    public static ZoneId parseZoneId(String timezone) {
        switch (timezone.toLowerCase()) {
            case "kst": return ZoneId.of("Asia/Seoul");
            case "utc": return ZoneId.of("UTC");
            case "est": return ZoneId.of("America/New_York");
            case "pst": return ZoneId.of("America/Los_Angeles");
            case "cet": return ZoneId.of("Europe/Paris");
            default: throw new IllegalArgumentException("지원하지 않는 시간대입니다.");
        }
    }
}
