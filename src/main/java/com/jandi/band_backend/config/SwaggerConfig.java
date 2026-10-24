package com.jandi.band_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${swagger.server-url:http://localhost:8080}")
    private String serverUrl;

    @Value("${swagger.server-description:로컬 서버}")
    private String serverDescription;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url(serverUrl).description(serverDescription)
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    private Info apiInfo() {
        return new Info()
                .title("Jandi Band Backend API");
    }

    @Bean
    public OpenApiCustomizer jsonNodeSchema() {
        // 시간표 JSON을 Jackson 내부 속성 없이 기존 자유 형식으로 공개
        return api -> {
            if (api.getComponents().getSchemas().containsKey("JsonNode")) {
                api.getComponents().getSchemas().put("JsonNode", new Schema<>());
            }
        };
    }
}
