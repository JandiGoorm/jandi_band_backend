-- Local development schema; compatible with production MySQL 26.10.0-cloud.
-- Schema definitions only; no production rows, comments or AUTO_INCREMENT counters.
-- Import into an EMPTY local database. Foreign key checks stay enabled.

SET NAMES utf8mb4;

CREATE TABLE `app_expiring_value` (
  `entry_key` varchar(191) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `entry_value` varchar(255) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  PRIMARY KEY (`entry_key`),
  KEY `idx_app_expiring_value_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `region` (
  `region_id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(20) NOT NULL,
  `name` varchar(50) NOT NULL,
  PRIMARY KEY (`region_id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `report_reason` (
  `report_reason_id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(50) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(255) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`report_reason_id`),
  UNIQUE KEY `UKohj0hdotq5okm4knubykdmu2d` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `university` (
  `university_id` int NOT NULL AUTO_INCREMENT,
  `university_code` char(7) NOT NULL,
  `name` varchar(100) NOT NULL,
  `region_id` int NOT NULL,
  `address` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`university_id`),
  UNIQUE KEY `university_code` (`university_code`),
  KEY `region_id` (`region_id`),
  CONSTRAINT `university_ibfk_1` FOREIGN KEY (`region_id`) REFERENCES `region` (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `club` (
  `club_id` int NOT NULL AUTO_INCREMENT,
  `chatroom_url` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text,
  `instagram_id` varchar(50) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `university_id` int DEFAULT NULL,
  PRIMARY KEY (`club_id`),
  KEY `FKmf82w0dtc98509noons2e6hww` (`university_id`),
  CONSTRAINT `FKmf82w0dtc98509noons2e6hww` FOREIGN KEY (`university_id`) REFERENCES `university` (`university_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `admin_role` enum('ADMIN','USER') NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `is_registered` bit(1) NOT NULL,
  `kakao_oauth_id` varchar(255) NOT NULL,
  `nickname` varchar(100) NOT NULL,
  `position` enum('BASS','DRUM','GUITAR','KEYBOARD','OTHER','VOCAL') DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `university_id` int DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UKqgeye1oqegt2pc4o6vpx7tngh` (`kakao_oauth_id`),
  KEY `FKg6bhc1g69lfy3mquw927rmr9m` (`university_id`),
  CONSTRAINT `FKg6bhc1g69lfy3mquw927rmr9m` FOREIGN KEY (`university_id`) REFERENCES `university` (`university_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `club_event` (
  `club_event_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text,
  `end_datetime` datetime(6) NOT NULL,
  `name` varchar(255) NOT NULL,
  `start_datetime` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `club_id` int NOT NULL,
  `creator_user_id` int NOT NULL,
  PRIMARY KEY (`club_event_id`),
  KEY `FKi2h9w0o5q2wxx1a6h1rgnc4bf` (`club_id`),
  KEY `FKf7n5ed0i71s14pm1o3us673iw` (`creator_user_id`),
  CONSTRAINT `FKf7n5ed0i71s14pm1o3us673iw` FOREIGN KEY (`creator_user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKi2h9w0o5q2wxx1a6h1rgnc4bf` FOREIGN KEY (`club_id`) REFERENCES `club` (`club_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `club_gal_photo` (
  `club_gal_photo_id` int NOT NULL AUTO_INCREMENT,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text,
  `image_url` varchar(512) NOT NULL,
  `is_pinned` bit(1) NOT NULL,
  `is_public` bit(1) NOT NULL,
  `uploaded_at` datetime(6) NOT NULL,
  `club_id` int NOT NULL,
  `uploader_user_id` int NOT NULL,
  PRIMARY KEY (`club_gal_photo_id`),
  KEY `FK3qxbkh62danvl0um5vef048il` (`club_id`),
  KEY `FKfqpw8pw8419a8r5fa9hn76n99` (`uploader_user_id`),
  CONSTRAINT `FK3qxbkh62danvl0um5vef048il` FOREIGN KEY (`club_id`) REFERENCES `club` (`club_id`),
  CONSTRAINT `FKfqpw8pw8419a8r5fa9hn76n99` FOREIGN KEY (`uploader_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `club_member` (
  `club_member_id` int NOT NULL AUTO_INCREMENT,
  `deleted_at` datetime(6) DEFAULT NULL,
  `joined_at` datetime(6) NOT NULL,
  `role` enum('BANNED','MEMBER','REPRESENTATIVE') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `club_id` int NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`club_member_id`),
  UNIQUE KEY `UKjosb9v9s0rolusorxeot51icq` (`club_id`,`user_id`),
  KEY `FKbkak0q9hvao01xs5gem9h52tf` (`user_id`),
  CONSTRAINT `FKbkak0q9hvao01xs5gem9h52tf` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKf6tl19ih8acrmheidn4xos2tx` FOREIGN KEY (`club_id`) REFERENCES `club` (`club_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `club_pending` (
  `club_pending_id` int NOT NULL AUTO_INCREMENT,
  `club_id` int NOT NULL,
  `user_id` int NOT NULL,
  `status` enum('PENDING','APPROVED','REJECTED','EXPIRED','CANCELED') NOT NULL DEFAULT 'PENDING',
  `processed_by` int DEFAULT NULL,
  `applied_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `processed_at` timestamp NULL DEFAULT NULL,
  `expires_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`club_pending_id`),
  UNIQUE KEY `uk_club_user_status` (`club_id`,`user_id`,`status`),
  KEY `processed_by` (`processed_by`),
  KEY `club_id_2` (`club_id`),
  KEY `user_id` (`user_id`),
  KEY `status` (`status`),
  KEY `expires_at` (`expires_at`),
  CONSTRAINT `club_pending_ibfk_1` FOREIGN KEY (`club_id`) REFERENCES `club` (`club_id`),
  CONSTRAINT `club_pending_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `club_pending_ibfk_3` FOREIGN KEY (`processed_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `club_photo` (
  `club_photo_id` int NOT NULL AUTO_INCREMENT,
  `deleted_at` datetime(6) DEFAULT NULL,
  `image_url` varchar(512) NOT NULL,
  `is_current` bit(1) NOT NULL,
  `uploaded_at` datetime(6) NOT NULL,
  `club_id` int NOT NULL,
  PRIMARY KEY (`club_photo_id`),
  UNIQUE KEY `UK4k7c41t9i4jpdev5iv4ok35lx` (`club_id`,`is_current`),
  CONSTRAINT `FK6xcqof0e76efu5alqi0vrlvrk` FOREIGN KEY (`club_id`) REFERENCES `club` (`club_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notice` (
  `notice_id` int NOT NULL AUTO_INCREMENT,
  `creator_user_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` text,
  `start_datetime` datetime NOT NULL,
  `end_datetime` datetime NOT NULL,
  `is_paused` tinyint(1) NOT NULL DEFAULT '0',
  `image_url` varchar(512) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`notice_id`),
  KEY `creator_user_id` (`creator_user_id`),
  CONSTRAINT `notice_ibfk_1` FOREIGN KEY (`creator_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `promo` (
  `promo_id` int NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL,
  `admission_fee` decimal(10,2) DEFAULT NULL,
  `comment_count` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text,
  `event_datetime` datetime(6) DEFAULT NULL,
  `latitude` decimal(17,14) DEFAULT NULL,
  `like_count` int NOT NULL,
  `location` varchar(255) DEFAULT NULL,
  `longitude` decimal(17,14) DEFAULT NULL,
  `team_name` varchar(255) NOT NULL,
  `title` varchar(255) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `view_count` int NOT NULL,
  `club_id` int DEFAULT NULL,
  `creator_user_id` int NOT NULL,
  PRIMARY KEY (`promo_id`),
  KEY `FKdv7ocped3b427rlsl3m1kvb9t` (`club_id`),
  KEY `FKl3cwaym3ngxuoyjkwe23xnj6r` (`creator_user_id`),
  CONSTRAINT `FKdv7ocped3b427rlsl3m1kvb9t` FOREIGN KEY (`club_id`) REFERENCES `club` (`club_id`),
  CONSTRAINT `FKl3cwaym3ngxuoyjkwe23xnj6r` FOREIGN KEY (`creator_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `team` (
  `team_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text,
  `name` varchar(100) NOT NULL,
  `suggested_schedule_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `club_id` int NOT NULL,
  `creator_user_id` int NOT NULL,
  PRIMARY KEY (`team_id`),
  KEY `FKnl01gosacvic5nvy7gq3h7v2y` (`club_id`),
  KEY `FKrajio5uh7fth8vkmmp8vwhnkg` (`creator_user_id`),
  CONSTRAINT `FKnl01gosacvic5nvy7gq3h7v2y` FOREIGN KEY (`club_id`) REFERENCES `club` (`club_id`),
  CONSTRAINT `FKrajio5uh7fth8vkmmp8vwhnkg` FOREIGN KEY (`creator_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_photo` (
  `user_photo_id` int NOT NULL AUTO_INCREMENT,
  `deleted_at` datetime(6) DEFAULT NULL,
  `image_url` varchar(512) NOT NULL,
  `is_current` bit(1) NOT NULL,
  `uploaded_at` datetime(6) NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`user_photo_id`),
  UNIQUE KEY `UKt5kxuhoe3wxrtdj2vnvxvm2vr` (`user_id`,`is_current`),
  CONSTRAINT `FK8h4itffk04ejkbshbys7r3lce` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_timetable` (
  `user_timetable_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `user_timetable_data` json NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`user_timetable_id`),
  KEY `FK2j2mkw05yenbaojr8nhd2ma2h` (`user_id`),
  CONSTRAINT `FK2j2mkw05yenbaojr8nhd2ma2h` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `poll` (
  `poll_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `end_datetime` datetime(6) NOT NULL,
  `start_datetime` datetime(6) NOT NULL,
  `title` varchar(255) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `club_id` int NOT NULL,
  `creator_user_id` int NOT NULL,
  `team_id` int DEFAULT NULL,
  PRIMARY KEY (`poll_id`),
  KEY `FKryqcgg9f2lgfmf3yjwev74v6a` (`club_id`),
  KEY `FK6d7037wgftrshd6i9c5f29tam` (`creator_user_id`),
  KEY `FK1hkf0afxm23jj1dns93qo190v` (`team_id`),
  CONSTRAINT `FK1hkf0afxm23jj1dns93qo190v` FOREIGN KEY (`team_id`) REFERENCES `team` (`team_id`),
  CONSTRAINT `FK6d7037wgftrshd6i9c5f29tam` FOREIGN KEY (`creator_user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKryqcgg9f2lgfmf3yjwev74v6a` FOREIGN KEY (`club_id`) REFERENCES `club` (`club_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `promo_comment` (
  `promo_comment_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `creator_user_id` int NOT NULL,
  `promo_id` int NOT NULL,
  PRIMARY KEY (`promo_comment_id`),
  KEY `FK4il8bmm22x6sgwgcf3lmvrrwl` (`creator_user_id`),
  KEY `FKbd4hmsqxqc9ep8w6fnv64y55x` (`promo_id`),
  CONSTRAINT `FK4il8bmm22x6sgwgcf3lmvrrwl` FOREIGN KEY (`creator_user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKbd4hmsqxqc9ep8w6fnv64y55x` FOREIGN KEY (`promo_id`) REFERENCES `promo` (`promo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `promo_like` (
  `promo_like_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `promo_id` int NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`promo_like_id`),
  UNIQUE KEY `UKg5m7fv07n2aoe7b5xk2am16ih` (`promo_id`,`user_id`),
  KEY `FK5ep3n03ykpyjo7gb311kx2g94` (`user_id`),
  CONSTRAINT `FK5ep3n03ykpyjo7gb311kx2g94` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKr0gbal6dy03fpeurcgxf01d6l` FOREIGN KEY (`promo_id`) REFERENCES `promo` (`promo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `promo_photo` (
  `promo_photo_id` int NOT NULL AUTO_INCREMENT,
  `deleted_at` datetime(6) DEFAULT NULL,
  `image_url` varchar(512) NOT NULL,
  `is_current` bit(1) NOT NULL,
  `uploaded_at` datetime(6) NOT NULL,
  `promo_id` int NOT NULL,
  `uploader_user_id` int NOT NULL,
  PRIMARY KEY (`promo_photo_id`),
  UNIQUE KEY `UKprlacwoiua7u0taicoirkellj` (`promo_id`,`is_current`),
  KEY `FKhb4iy51sqklhpb1prfjgerdx1` (`uploader_user_id`),
  CONSTRAINT `FKhb4iy51sqklhpb1prfjgerdx1` FOREIGN KEY (`uploader_user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKpjbmksm9jt2w6iydu71xla6f3` FOREIGN KEY (`promo_id`) REFERENCES `promo` (`promo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `promo_report` (
  `promo_report_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `description` text,
  `promo_id` int NOT NULL,
  `report_reason_id` int NOT NULL,
  `reporter_user_id` int NOT NULL,
  PRIMARY KEY (`promo_report_id`),
  KEY `FK3mth6gmu5ssywyob30o17vo8h` (`promo_id`),
  KEY `FK8wnu2ks24yf5vri0hpr347ac1` (`report_reason_id`),
  KEY `FKn42xtt0lg8a0lmmvs45jdvs0d` (`reporter_user_id`),
  CONSTRAINT `FK3mth6gmu5ssywyob30o17vo8h` FOREIGN KEY (`promo_id`) REFERENCES `promo` (`promo_id`),
  CONSTRAINT `FK8wnu2ks24yf5vri0hpr347ac1` FOREIGN KEY (`report_reason_id`) REFERENCES `report_reason` (`report_reason_id`),
  CONSTRAINT `FKn42xtt0lg8a0lmmvs45jdvs0d` FOREIGN KEY (`reporter_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `team_event` (
  `team_event_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text,
  `end_datetime` datetime(6) NOT NULL,
  `name` varchar(255) NOT NULL,
  `no_position` enum('BASS','DRUM','GUITAR','KEYBOARD','NONE','VOCAL') DEFAULT NULL,
  `start_datetime` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `creator_user_id` int NOT NULL,
  `team_id` int NOT NULL,
  PRIMARY KEY (`team_event_id`),
  KEY `FKbcl08nnap42oq03dso9mjy9x7` (`creator_user_id`),
  KEY `FK46rv5umqrb19j7xskqa90r4fp` (`team_id`),
  CONSTRAINT `FK46rv5umqrb19j7xskqa90r4fp` FOREIGN KEY (`team_id`) REFERENCES `team` (`team_id`),
  CONSTRAINT `FKbcl08nnap42oq03dso9mjy9x7` FOREIGN KEY (`creator_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `team_member` (
  `team_member_id` int NOT NULL AUTO_INCREMENT,
  `deleted_at` datetime(6) DEFAULT NULL,
  `joined_at` datetime(6) NOT NULL,
  `timetable_data` json DEFAULT NULL,
  `updated_timetable_at` datetime(6) DEFAULT NULL,
  `team_id` int NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`team_member_id`),
  UNIQUE KEY `UKj6w13vhqtsr7j4ood3u049ttc` (`team_id`,`user_id`),
  KEY `FKcwlc6ivvbf5svve5fmbg71vks` (`user_id`),
  CONSTRAINT `FK9ubp79ei4tv4crd0r9n7u5i6e` FOREIGN KEY (`team_id`) REFERENCES `team` (`team_id`),
  CONSTRAINT `FKcwlc6ivvbf5svve5fmbg71vks` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `poll_song` (
  `poll_song_id` int NOT NULL AUTO_INCREMENT,
  `artist_name` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text,
  `song_name` varchar(255) NOT NULL,
  `youtube_url` varchar(255) DEFAULT NULL,
  `poll_id` int NOT NULL,
  `suggester_user_id` int NOT NULL,
  PRIMARY KEY (`poll_song_id`),
  KEY `FKc7vg4l2m50h8qjqdf4x92ww58` (`poll_id`),
  KEY `FKgfq8tkp2it9pkawvfoe37l72q` (`suggester_user_id`),
  CONSTRAINT `FKc7vg4l2m50h8qjqdf4x92ww58` FOREIGN KEY (`poll_id`) REFERENCES `poll` (`poll_id`),
  CONSTRAINT `FKgfq8tkp2it9pkawvfoe37l72q` FOREIGN KEY (`suggester_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `promo_comment_like` (
  `promo_comment_like_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `promo_comment_id` int NOT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`promo_comment_like_id`),
  UNIQUE KEY `UKah8v4ty5o7hm59h6t7giy18bl` (`promo_comment_id`,`user_id`),
  KEY `FK2d0vqn4uvi1m6ei8jda7xq6c7` (`user_id`),
  CONSTRAINT `FK2d0vqn4uvi1m6ei8jda7xq6c7` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FK5xn3r9mjgq8iwuubpd5asxknb` FOREIGN KEY (`promo_comment_id`) REFERENCES `promo_comment` (`promo_comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `promo_comment_report` (
  `promo_comment_report_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `description` text,
  `promo_comment_id` int NOT NULL,
  `report_reason_id` int NOT NULL,
  `reporter_user_id` int NOT NULL,
  PRIMARY KEY (`promo_comment_report_id`),
  KEY `FKpovfdgksesjrh4ygxmu9ngty7` (`promo_comment_id`),
  KEY `FK5wfvxr2snb0ypemf8brj6j6so` (`report_reason_id`),
  KEY `FKalwtpj6gelay8m7iqyxjoalcf` (`reporter_user_id`),
  CONSTRAINT `FK5wfvxr2snb0ypemf8brj6j6so` FOREIGN KEY (`report_reason_id`) REFERENCES `report_reason` (`report_reason_id`),
  CONSTRAINT `FKalwtpj6gelay8m7iqyxjoalcf` FOREIGN KEY (`reporter_user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKpovfdgksesjrh4ygxmu9ngty7` FOREIGN KEY (`promo_comment_id`) REFERENCES `promo_comment` (`promo_comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `vote` (
  `vote_id` int NOT NULL AUTO_INCREMENT,
  `voted_at` datetime(6) NOT NULL,
  `voted_mark` enum('CANT','DISLIKE','HAJJ','LIKE') NOT NULL,
  `poll_song_id` int NOT NULL,
  `user_id` int NOT NULL,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`vote_id`),
  UNIQUE KEY `UKbg9rv3kdws15anjt4308v27ut` (`poll_song_id`,`user_id`),
  KEY `FKkmvvqilx49120p47nr9t56omf` (`user_id`),
  CONSTRAINT `FKc7jnxly5ueerqvv7hgxtvfnnj` FOREIGN KEY (`poll_song_id`) REFERENCES `poll_song` (`poll_song_id`),
  CONSTRAINT `FKkmvvqilx49120p47nr9t56omf` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
