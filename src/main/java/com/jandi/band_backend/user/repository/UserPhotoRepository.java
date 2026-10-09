package com.jandi.band_backend.user.repository;

import com.jandi.band_backend.user.entity.UserPhoto;
import com.jandi.band_backend.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserPhotoRepository extends JpaRepository<UserPhoto, Long> {
    UserPhoto findByUser(Users user);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM UserPhoto p WHERE p.user = :user AND p.deletedAt IS NULL")
    Optional<UserPhoto> findForUpdate(@Param("user") Users user);

    @Modifying
    @Query("UPDATE UserPhoto up SET up.deletedAt = :deletedAt WHERE up.user.id = :userId AND up.deletedAt IS NULL")
    int softDeleteByUserId(@Param("userId") Integer userId, @Param("deletedAt") LocalDateTime deletedAt);
}
