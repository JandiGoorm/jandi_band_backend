package com.jandi.band_backend.user.repository;

import com.jandi.band_backend.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Integer> {
    Optional<Users> findById(Integer id);
    Optional<Users> findByKakaoOauthId(String kakaoOauthId);

    Optional<Users> findByKakaoOauthIdAndDeletedAtIsNull(String kakaoOauthId);

    List<Users> findAllByDeletedAtBefore(LocalDateTime deletedAtBefore);
}
