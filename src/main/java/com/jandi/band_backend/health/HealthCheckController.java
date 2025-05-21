package com.jandi.band_backend.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthCheckController {

    private final Instant startTime = Instant.now();
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("uptime", Duration.between(startTime, Instant.now()).toString());
        response.put("serverTime", formatter.format(Instant.now()));
        return response;
    }
}
