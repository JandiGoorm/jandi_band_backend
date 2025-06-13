package com.jandi.band_backend.user.repository;

import com.jandi.band_backend.user.entity.UserTimetable;
import com.jandi.band_backend.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTimetableRepository extends JpaRepository<UserTimetable, Integer> {
    List<UserTimetable> findByUserAndDeletedAtIsNull(Users user);

    Optional<UserTimetable> findByIdAndDeletedAtIsNull(Integer timetableId);

    @Query("SELECT ut FROM UserTimetable ut JOIN FETCH ut.user WHERE ut.id = :timetableId AND ut.deletedAt IS NULL")
    Optional<UserTimetable> findByIdWithUserAndDeletedAtIsNull(@Param("timetableId") Integer timetableId);

    @Modifying
    @Query("UPDATE UserTimetable ut SET ut.deletedAt = :deletedAt WHERE ut.user.id = :userId AND ut.deletedAt IS NULL")
    int softDeleteByUserId(@Param("userId") Integer userId, @Param("deletedAt") LocalDateTime deletedAt);

    List<UserTimetable> findAllByUser(Users user);

    int deleteAllByUser(Users user);
}
