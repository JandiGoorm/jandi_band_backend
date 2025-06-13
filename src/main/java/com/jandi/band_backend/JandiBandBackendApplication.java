package com.jandi.band_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class JandiBandBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(JandiBandBackendApplication.class, args);
    }

}
