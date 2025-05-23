package com.jandi.band_backend.team.repository;

import com.jandi.band_backend.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Integer> {
    
    @Query("SELECT tm FROM TeamMember tm WHERE tm.user.id = :userId AND tm.team.deletedAt IS NULL ORDER BY tm.joinedAt DESC")
    List<TeamMember> findByUserId(@Param("userId") Integer userId);
    
    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId")
    List<TeamMember> findByTeamId(@Param("teamId") Integer teamId);
} 