-- Synthetic development data only. Import ONCE after 001_schema.sql into an empty local database.
-- No production users, OAuth identifiers, sessions, credentials or image URLs are included.
-- Fictional OAuth IDs cannot authenticate with the real Kakao service.

SET NAMES utf8mb4;
SET @mock_now = CURRENT_TIMESTAMP(6);
SET @mock_image = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIj48cmVjdCB3aWR0aD0iMzIwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iIzQzMzhjYSIvPjx0ZXh0IHg9IjE2MCIgeT0iMTA4IiBmaWxsPSJ3aGl0ZSIgZm9udC1zaXplPSIyNCIgdGV4dC1hbmNob3I9Im1pZGRsZSI+TE9DQUwgREVNTzwvdGV4dD48L3N2Zz4=';
START TRANSACTION;

INSERT INTO `region` (`region_id`, `code`, `name`) VALUES
  (1, 'DEMO_NORTH', '가상 북부'),
  (2, 'DEMO_SOUTH', '가상 남부'),
  (3, 'DEMO_ISLAND', '가상 섬');

INSERT INTO `university` (`university_id`, `university_code`, `name`, `region_id`, `address`) VALUES
  (1, 'MOCK001', '가상 리듬대학교', 1, '개발용 가상 주소 1'),
  (2, 'MOCK002', '가상 화음대학교', 2, '개발용 가상 주소 2'),
  (3, 'MOCK003', '가상 소리대학교', 3, NULL);

INSERT INTO `users` (`user_id`, `admin_role`, `kakao_oauth_id`, `nickname`, `position`, `university_id`, `is_registered`, `created_at`, `updated_at`, `deleted_at`) VALUES
  (1, 'USER', 'image-test-browser-dev-owner', '개발 보컬', 'VOCAL', 1, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (2, 'USER', 'image-test-browser-dev-guitar', '개발 기타', 'GUITAR', 1, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (3, 'USER', 'image-test-browser-dev-bass', '개발 베이스', 'BASS', 1, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (4, 'USER', 'image-test-browser-dev-drum', '개발 드럼', 'DRUM', 2, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (5, 'USER', 'image-test-browser-dev-keyboard', '개발 건반', 'KEYBOARD', 2, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (6, 'USER', 'image-test-browser-dev-guest', '개발 게스트', 'OTHER', 3, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (7, 'ADMIN', 'image-test-browser-dev-admin', '개발 관리자', 'OTHER', 1, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (8, 'USER', 'image-test-browser-dev-new', '개발 가입대기', NULL, NULL, 0, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL);

INSERT INTO `user_photo` (`user_photo_id`, `user_id`, `image_url`, `is_current`, `uploaded_at`, `deleted_at`) VALUES
  (1, 1, @mock_image, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL),
  (2, 2, @mock_image, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL),
  (3, 3, @mock_image, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL),
  (4, 4, @mock_image, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL),
  (5, 5, @mock_image, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL),
  (6, 6, @mock_image, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL),
  (7, 7, @mock_image, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL),
  (8, 8, @mock_image, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL);

INSERT INTO `club` (`club_id`, `name`, `description`, `university_id`, `chatroom_url`, `instagram_id`, `created_at`, `updated_at`, `deleted_at`) VALUES
  (1, '개발용 리듬 밴드', '보컬·기타·베이스가 함께하는 가상 동아리입니다.', 1, NULL, NULL, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (2, '개발용 화음 밴드', '드럼·건반 중심의 두 번째 가상 동아리입니다.', 2, NULL, NULL, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (3, '삭제된 개발 동아리', '목록 필터 확인용으로만 남긴 가상 데이터입니다.', 3, NULL, NULL, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, DATE_SUB(@mock_now, INTERVAL 1 DAY));

INSERT INTO `club_photo` (`club_photo_id`, `club_id`, `image_url`, `is_current`, `uploaded_at`, `deleted_at`) VALUES
  (1, 1, @mock_image, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL),
  (2, 2, @mock_image, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL),
  (3, 3, @mock_image, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), DATE_SUB(@mock_now, INTERVAL 1 DAY));

INSERT INTO `club_member` (`club_member_id`, `club_id`, `user_id`, `role`, `joined_at`, `updated_at`, `deleted_at`) VALUES
  (1, 1, 1, 'REPRESENTATIVE', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (2, 1, 2, 'MEMBER', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (3, 1, 3, 'MEMBER', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (4, 1, 4, 'MEMBER', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (5, 1, 5, 'MEMBER', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (6, 1, 6, 'BANNED', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, DATE_SUB(@mock_now, INTERVAL 1 DAY)),
  (7, 2, 4, 'REPRESENTATIVE', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (8, 2, 5, 'MEMBER', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (9, 2, 2, 'MEMBER', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, DATE_SUB(@mock_now, INTERVAL 1 DAY)),
  (10, 3, 6, 'REPRESENTATIVE', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, DATE_SUB(@mock_now, INTERVAL 1 DAY));

INSERT INTO `club_pending` (`club_pending_id`, `club_id`, `user_id`, `status`, `processed_by`, `applied_at`, `processed_at`, `expires_at`) VALUES
  (1, 2, 3, 'PENDING', NULL, @mock_now, NULL, DATE_ADD(@mock_now, INTERVAL 7 DAY)),
  (2, 1, 2, 'APPROVED', 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL),
  (3, 2, 6, 'REJECTED', 4, DATE_SUB(@mock_now, INTERVAL 30 DAY), DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL);

INSERT INTO `team` (`team_id`, `club_id`, `creator_user_id`, `name`, `description`, `suggested_schedule_at`, `created_at`, `updated_at`, `deleted_at`) VALUES
  (1, 1, 1, '개발용 봄 합주팀', '투표와 시간표를 확인할 수 있는 가상 팀입니다.', @mock_now, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (2, 2, 4, '개발용 저녁 합주팀', '다른 동아리에 속한 가상 팀입니다.', NULL, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (3, 1, 1, '삭제된 개발 팀', '삭제 필터 확인용입니다.', NULL, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, DATE_SUB(@mock_now, INTERVAL 1 DAY));

INSERT INTO `team_member` (`team_member_id`, `team_id`, `user_id`, `joined_at`, `timetable_data`, `updated_timetable_at`, `deleted_at`) VALUES
  (1, 1, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), '{"Mon":["18:00","18:30","19:00"],"Tue":[],"Wed":["19:00","19:30"],"Thu":[],"Fri":[],"Sat":[],"Sun":[]}', @mock_now, NULL),
  (2, 1, 2, DATE_SUB(@mock_now, INTERVAL 30 DAY), '{"Mon":[],"Tue":["18:00","18:30"],"Wed":["19:00","19:30"],"Thu":[],"Fri":[],"Sat":[],"Sun":[]}', @mock_now, NULL),
  (3, 1, 3, DATE_SUB(@mock_now, INTERVAL 30 DAY), '{"Mon":[],"Tue":[],"Wed":[],"Thu":[],"Fri":[],"Sat":[],"Sun":[]}', @mock_now, NULL),
  (4, 2, 4, DATE_SUB(@mock_now, INTERVAL 30 DAY), '{"Mon":["18:00","18:30","19:00"],"Tue":[],"Wed":["19:00","19:30"],"Thu":[],"Fri":[],"Sat":[],"Sun":[]}', @mock_now, NULL),
  (5, 2, 5, DATE_SUB(@mock_now, INTERVAL 30 DAY), '{"Mon":[],"Tue":["18:00","18:30"],"Wed":["19:00","19:30"],"Thu":[],"Fri":[],"Sat":[],"Sun":[]}', @mock_now, NULL),
  (6, 3, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL, NULL, DATE_SUB(@mock_now, INTERVAL 1 DAY));

INSERT INTO `user_timetable` (`user_timetable_id`, `user_id`, `name`, `user_timetable_data`, `created_at`, `updated_at`, `deleted_at`) VALUES
  (1, 1, '개발용 평일 시간표', '{"Mon":["18:00","18:30","19:00"],"Tue":[],"Wed":["19:00","19:30"],"Thu":[],"Fri":[],"Sat":[],"Sun":[]}', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (2, 1, '개발용 빈 시간표', '{"Mon":[],"Tue":[],"Wed":[],"Thu":[],"Fri":[],"Sat":[],"Sun":[]}', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (3, 2, '개발용 기타 시간표', '{"Mon":[],"Tue":["18:00","18:30"],"Wed":["19:00","19:30"],"Thu":[],"Fri":[],"Sat":[],"Sun":[]}', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (4, 4, '개발용 드럼 시간표', '{"Mon":["18:00","18:30","19:00"],"Tue":[],"Wed":["19:00","19:30"],"Thu":[],"Fri":[],"Sat":[],"Sun":[]}', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL);

INSERT INTO `club_event` (`club_event_id`, `club_id`, `creator_user_id`, `name`, `description`, `start_datetime`, `end_datetime`, `created_at`, `updated_at`, `deleted_at`) VALUES
  (1, 1, 1, '개발용 정기 모임', '일정 상세 화면 확인용입니다.', DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 3 DAY), INTERVAL 18 HOUR), DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 3 DAY), INTERVAL 20 HOUR), DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (2, 2, 4, '개발용 합동 공연', '두 번째 동아리의 가상 일정입니다.', DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 7 DAY), INTERVAL 19 HOUR), DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 7 DAY), INTERVAL 21 HOUR), DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (3, 1, 1, '취소된 개발 일정', '삭제 필터 확인용입니다.', DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 2 DAY), INTERVAL 18 HOUR), DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 2 DAY), INTERVAL 19 HOUR), DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, DATE_SUB(@mock_now, INTERVAL 1 DAY));

INSERT INTO `team_event` (`team_event_id`, `team_id`, `creator_user_id`, `name`, `description`, `no_position`, `start_datetime`, `end_datetime`, `created_at`, `updated_at`, `deleted_at`) VALUES
  (1, 1, 1, '개발용 전체 합주', '포지션 누락 없는 합주입니다.', 'NONE', DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 4 DAY), INTERVAL 19 HOUR), DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 4 DAY), INTERVAL 21 HOUR), DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (2, 1, 1, '개발용 보컬 제외 합주', '보컬 제외 표시 확인용입니다.', 'VOCAL', DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 6 DAY), INTERVAL 18 HOUR), DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 6 DAY), INTERVAL 20 HOUR), DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (3, 2, 4, '개발용 리듬 연습', NULL, 'GUITAR', DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 5 DAY), INTERVAL 18 HOUR), DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 5 DAY), INTERVAL 20 HOUR), DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL);

INSERT INTO `club_gal_photo` (`club_gal_photo_id`, `club_id`, `uploader_user_id`, `image_url`, `description`, `is_pinned`, `is_public`, `uploaded_at`, `deleted_at`) VALUES
  (1, 1, 1, @mock_image, '공개·고정된 개발 갤러리 사진', 1, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL),
  (2, 1, 2, @mock_image, '멤버 전용 개발 갤러리 사진', 0, 0, @mock_now, NULL),
  (3, 2, 4, @mock_image, '다른 동아리의 공개 사진', 0, 1, @mock_now, NULL),
  (4, 1, 3, @mock_image, '삭제된 개발 사진', 0, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), DATE_SUB(@mock_now, INTERVAL 1 DAY));

INSERT INTO `poll` (`poll_id`, `club_id`, `creator_user_id`, `team_id`, `title`, `start_datetime`, `end_datetime`, `created_at`, `updated_at`, `deleted_at`) VALUES
  (1, 1, 1, 1, '개발용 다음 합주곡 투표', DATE_SUB(@mock_now, INTERVAL 1 DAY), DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 7 DAY), INTERVAL 23 HOUR), DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (2, 2, 4, NULL, '개발용 마감된 공연곡 투표', DATE_SUB(@mock_now, INTERVAL 10 DAY), DATE_SUB(@mock_now, INTERVAL 2 DAY), DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (3, 1, 1, NULL, '개발용 예정된 투표', DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 3 DAY), INTERVAL 9 HOUR), DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 10 DAY), INTERVAL 23 HOUR), DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL);

INSERT INTO `poll_song` (`poll_song_id`, `poll_id`, `suggester_user_id`, `song_name`, `artist_name`, `description`, `youtube_url`, `created_at`, `deleted_at`) VALUES
  (1, 1, 1, '가상곡 첫 번째', '가상 아티스트 A', '보컬 연습용 가상곡', NULL, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL),
  (2, 1, 2, '가상곡 두 번째', '가상 아티스트 B', '기타 연습용 가상곡', NULL, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL),
  (3, 1, 3, '가상곡 세 번째', NULL, NULL, NULL, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL),
  (4, 2, 4, '가상곡 지난 공연', '가상 아티스트 C', NULL, NULL, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL);

INSERT INTO `vote` (`vote_id`, `poll_song_id`, `user_id`, `voted_mark`, `voted_at`, `deleted_at`) VALUES
  (1, 1, 1, 'LIKE', @mock_now, NULL),
  (2, 1, 2, 'LIKE', @mock_now, NULL),
  (3, 1, 3, 'HAJJ', @mock_now, NULL),
  (4, 2, 1, 'DISLIKE', @mock_now, NULL),
  (5, 2, 2, 'CANT', @mock_now, NULL),
  (6, 2, 3, 'LIKE', @mock_now, NULL),
  (7, 4, 4, 'LIKE', DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL);

INSERT INTO `promo` (`promo_id`, `club_id`, `creator_user_id`, `team_name`, `title`, `description`, `address`, `location`, `latitude`, `longitude`, `admission_fee`, `event_datetime`, `view_count`, `like_count`, `comment_count`, `created_at`, `updated_at`, `deleted_at`) VALUES
  (1, 1, 1, '개발용 봄 합주팀', '개발용 합주 공연에 초대합니다', '실제 공연이 아닌 화면 개발용 게시글입니다.', NULL, '가상 연습실 A', NULL, NULL, 0, DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 14 DAY), INTERVAL 19 HOUR), 12, 3, 2, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (2, 2, 4, '개발용 저녁 합주팀', '개발용 유료 공연 안내', '요금과 댓글 표시를 확인하는 가상 게시글입니다.', NULL, '가상 공연장 B', NULL, NULL, 5000, DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 21 DAY), INTERVAL 20 HOUR), 4, 1, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (3, NULL, 6, '개발용 게스트 팀', '삭제된 개발 홍보글', '삭제 필터 확인용입니다.', NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 0, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, DATE_SUB(@mock_now, INTERVAL 1 DAY));

INSERT INTO `promo_photo` (`promo_photo_id`, `promo_id`, `uploader_user_id`, `image_url`, `is_current`, `uploaded_at`, `deleted_at`) VALUES
  (1, 1, 1, @mock_image, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL),
  (2, 2, 4, @mock_image, 1, DATE_SUB(@mock_now, INTERVAL 30 DAY), NULL);

INSERT INTO `promo_comment` (`promo_comment_id`, `promo_id`, `creator_user_id`, `description`, `created_at`, `updated_at`, `deleted_at`) VALUES
  (1, 1, 2, '개발용 댓글: 공연을 기대합니다.', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (2, 1, 3, '개발용 댓글: 입장 시간도 알려주세요.', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (3, 2, 5, '개발용 댓글: 건반 연주가 기대됩니다.', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (4, 1, 6, '삭제된 개발 댓글', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, DATE_SUB(@mock_now, INTERVAL 1 DAY));

INSERT INTO `promo_like` (`promo_like_id`, `promo_id`, `user_id`, `created_at`) VALUES
  (1, 1, 2, @mock_now),
  (2, 1, 3, @mock_now),
  (3, 1, 4, @mock_now),
  (4, 2, 1, @mock_now);

INSERT INTO `promo_comment_like` (`promo_comment_like_id`, `promo_comment_id`, `user_id`, `created_at`) VALUES
  (1, 1, 1, @mock_now),
  (2, 1, 3, @mock_now),
  (3, 3, 4, @mock_now);

INSERT INTO `report_reason` (`report_reason_id`, `code`, `description`, `created_at`, `updated_at`) VALUES
  (1, 'SPAM', '개발용: 광고·도배', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now),
  (2, 'ABUSE', '개발용: 부적절한 내용', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now),
  (3, 'OTHER', '개발용: 기타 사유', DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now);

INSERT INTO `promo_report` (`promo_report_id`, `promo_id`, `reporter_user_id`, `report_reason_id`, `description`, `created_at`) VALUES
  (1, 2, 6, 3, '신고 화면 확인용 가상 신고입니다.', @mock_now);

INSERT INTO `promo_comment_report` (`promo_comment_report_id`, `promo_comment_id`, `reporter_user_id`, `report_reason_id`, `description`, `created_at`) VALUES
  (1, 2, 5, 2, '댓글 신고 화면 확인용 가상 신고입니다.', @mock_now);

INSERT INTO `notice` (`notice_id`, `creator_user_id`, `title`, `content`, `start_datetime`, `end_datetime`, `is_paused`, `image_url`, `created_at`, `updated_at`, `deleted_at`) VALUES
  (1, 7, '개발용 활성 공지', '모든 정보는 개발용 가상 데이터입니다.', DATE_SUB(@mock_now, INTERVAL 1 DAY), DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 30 DAY), INTERVAL 23 HOUR), 0, NULL, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (2, 7, '개발용 예정 공지', '게시 예정 상태를 확인합니다.', DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 3 DAY), INTERVAL 9 HOUR), DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 14 DAY), INTERVAL 23 HOUR), 0, NULL, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL),
  (3, 7, '개발용 중지 공지', '일시중지 상태를 확인합니다.', DATE_SUB(@mock_now, INTERVAL 1 DAY), DATE_ADD(DATE_ADD(CURRENT_DATE(), INTERVAL 30 DAY), INTERVAL 23 HOUR), 1, NULL, DATE_SUB(@mock_now, INTERVAL 30 DAY), @mock_now, NULL);

-- app_expiring_value intentionally stays empty: tokens, invite codes and blacklists are created by the app.
COMMIT;
SET @mock_now = NULL;
SET @mock_image = NULL;
