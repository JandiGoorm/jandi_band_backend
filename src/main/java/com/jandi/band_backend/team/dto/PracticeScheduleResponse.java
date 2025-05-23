package com.jandi.band_backend.team.dto;

import com.jandi.band_backend.team.entity.TeamEvent;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
@Setter
public class PracticeScheduleResponse {
    private Integer id;
    private Integer teamId;
    private String teamName;
    private String songName;
    private String artistName;
    private String youtubeUrl;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String location;
    private String address;
    private String additionalDescription;
    private Integer creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TeamEventParticipantResponse> participants;

    public static PracticeScheduleResponse from(TeamEvent teamEvent) {
        PracticeScheduleResponse response = new PracticeScheduleResponse();
        response.setId(teamEvent.getId());
        response.setTeamId(teamEvent.getTeam().getId());
        response.setTeamName(teamEvent.getTeam().getName());
        
        // name에서 곡명과 아티스트명 파싱 ("곡명 - 아티스트명" 형태)
        String[] songInfo = parseSongInfo(teamEvent.getName());
        response.setSongName(songInfo[0]);
        response.setArtistName(songInfo[1]);
        
        // description에서 YouTube URL과 추가 설명 파싱
        String[] descInfo = parseDescription(teamEvent.getDescription());
        response.setYoutubeUrl(descInfo[0]);
        response.setAdditionalDescription(descInfo[1]);
        
        response.setStartDatetime(teamEvent.getStartDatetime());
        response.setEndDatetime(teamEvent.getEndDatetime());
        response.setLocation(teamEvent.getLocation());
        response.setAddress(teamEvent.getAddress());
        response.setCreatorId(teamEvent.getCreator().getId());
        response.setCreatorName(teamEvent.getCreator().getNickname());
        response.setCreatedAt(teamEvent.getCreatedAt());
        response.setUpdatedAt(teamEvent.getUpdatedAt());
        response.setParticipants(teamEvent.getParticipants().stream()
                .map(TeamEventParticipantResponse::from)
                .collect(Collectors.toList()));
        return response;
    }
    
    private static String[] parseSongInfo(String name) {
        // "곡명 - 아티스트명" 형태에서 파싱
        if (name != null && name.contains(" - ")) {
            String[] parts = name.split(" - ", 2);
            return new String[]{parts[0].trim(), parts[1].trim()};
        }
        return new String[]{name != null ? name : "", ""};
    }
    
    private static String[] parseDescription(String description) {
        if (description == null) {
            return new String[]{"", ""};
        }
        
        // YouTube URL 패턴 찾기
        Pattern youtubePattern = Pattern.compile("(https?://)?(www\\.)?(youtube\\.com/watch\\?v=|youtu\\.be/)[\\w-]+");
        Matcher matcher = youtubePattern.matcher(description);
        
        String youtubeUrl = "";
        String additionalDesc = description;
        
        if (matcher.find()) {
            youtubeUrl = matcher.group();
            additionalDesc = description.replace(youtubeUrl, "").trim();
        }
        
        return new String[]{youtubeUrl, additionalDesc};
    }
} 