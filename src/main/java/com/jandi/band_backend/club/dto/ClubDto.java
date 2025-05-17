package com.jandi.band_backend.club.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public class ClubDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "동아리 이름은 필수입니다")
        @Size(max = 100, message = "동아리 이름은 100자 이내여야 합니다")
        private String name;

        @NotBlank(message = "대학은 필수입니다")
        private List<Integer> universityIds;

        @Size(max = 255, message = "카카오톡 채팅방 링크는 255자 이내여야 합니다")
        private String chatroomUrl;

        private String description;

        @Size(max = 50, message = "인스타그램 아이디는 50자 이내여야 합니다")
        private String instagramId;

        private String photoUrl;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Integer id;
        private String name;
        private List<UniversityDto.SimpleResponse> universities;
        private String chatroomUrl;
        private String description;
        private String instagramId;
        private String photoUrl;
        private Integer memberCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleResponse {
        private Integer id;
        private String name;
        private List<String> universityNames;
        private String photoUrl;
        private Integer memberCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @Size(max = 100, message = "동아리 이름은 100자 이내여야 합니다")
        private String name;

        private List<Integer> universityIds;

        @Size(max = 255, message = "카카오톡 채팅방 링크는 255자 이내여야 합니다")
        private String chatroomUrl;

        private String description;

        @Size(max = 50, message = "인스타그램 아이디는 50자 이내여야 합니다")
        private String instagramId;

        private String photoUrl;
    }
}
