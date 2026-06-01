-- caro.sql
-- Schema for "Hệ thống game cờ caro" (MySQL 8.0+)
-- Covers use cases: đăng nhập/đăng ký, hồ sơ cá nhân, phòng chơi, tham gia phòng,
-- chơi online/offline (vs máy / 2 người cục bộ), chat phòng, bảng xếp hạng, quản trị & log.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE DATABASE IF NOT EXISTS defaultdb
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE defaultdb;

-- =========================================================
-- Notes
-- - Không lưu mật khẩu plaintext: cột password_hash lưu bcrypt/argon2 (từ ứng dụng).
-- - Các bảng dùng InnoDB + khóa ngoại.
-- - Nếu muốn reset dữ liệu DEV: tự DROP TABLE theo thứ tự (tắt FK checks) trước khi tạo lại.
-- =========================================================

-- -----------------------------
-- 1) Users / Auth / Profile
-- -----------------------------
CREATE TABLE IF NOT EXISTS users (
                                     id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                     username        VARCHAR(50)      NOT NULL,
    email           VARCHAR(255)     NULL,
    password_hash   VARCHAR(255)     NOT NULL,
    role            ENUM('PLAYER','ADMIN') NOT NULL DEFAULT 'PLAYER',
    status          ENUM('ACTIVE','DISABLED','BANNED') NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    last_login_at   DATETIME(3)      NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email (email)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_profiles (
                                             user_id         BIGINT UNSIGNED NOT NULL,
                                             display_name    VARCHAR(100)     NULL,
    full_name       VARCHAR(120)     NULL,
    phone           VARCHAR(20)      NULL,
    avatar_url      VARCHAR(512)     NULL,
    bio             VARCHAR(500)     NULL,
    gender          ENUM('UNKNOWN','MALE','FEMALE','OTHER') NOT NULL DEFAULT 'UNKNOWN',
    date_of_birth   DATE             NULL,
    address         VARCHAR(255)     NULL,
    created_at      DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_profiles_user
    FOREIGN KEY (user_id) REFERENCES users(id)
                                                                           ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Optional: lưu session/token (nếu không dùng HttpSession thuần)
CREATE TABLE IF NOT EXISTS user_sessions (
                                             id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                             user_id         BIGINT UNSIGNED NOT NULL,
                                             session_token   CHAR(64)        NOT NULL,
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    expires_at      DATETIME(3)     NOT NULL,
    revoked_at      DATETIME(3)     NULL,
    ip              VARCHAR(45)     NULL,
    user_agent      VARCHAR(255)    NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_user_sessions_token (session_token),
    KEY idx_user_sessions_user (user_id),
    CONSTRAINT fk_user_sessions_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------
-- 2) Rooms / Membership / Chat
-- -----------------------------
CREATE TABLE IF NOT EXISTS rooms (
                                     id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                     room_code         VARCHAR(12)      NOT NULL,
    room_name         VARCHAR(100)     NULL,
    owner_user_id     BIGINT UNSIGNED  NOT NULL,
    status            ENUM('OPEN','IN_GAME','CLOSED') NOT NULL DEFAULT 'OPEN',
    is_private        TINYINT(1)       NOT NULL DEFAULT 0,
    room_password_hash VARCHAR(255)    NULL,
    max_players       TINYINT UNSIGNED NOT NULL DEFAULT 2,
    board_size        TINYINT UNSIGNED NOT NULL DEFAULT 15,
    created_at        DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)      NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_rooms_room_code (room_code),
    KEY idx_rooms_owner (owner_user_id),
    KEY idx_rooms_status (status),

    CONSTRAINT fk_rooms_owner
    FOREIGN KEY (owner_user_id) REFERENCES users(id)
                                                                             ON DELETE RESTRICT,

    CONSTRAINT ck_rooms_max_players CHECK (max_players BETWEEN 2 AND 10),
    CONSTRAINT ck_rooms_board_size CHECK (board_size BETWEEN 5 AND 50)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS room_members (
                                            id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                            room_id         BIGINT UNSIGNED NOT NULL,
                                            user_id         BIGINT UNSIGNED NOT NULL,
                                            member_role     ENUM('PLAYER','SPECTATOR') NOT NULL DEFAULT 'PLAYER',
    seat_no         TINYINT UNSIGNED NULL,
    member_status   ENUM('JOINED','LEFT','KICKED') NOT NULL DEFAULT 'JOINED',
    joined_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    left_at         DATETIME(3) NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_room_members_room_user (room_id, user_id),
    KEY idx_room_members_room (room_id),
    KEY idx_room_members_user (user_id),

    CONSTRAINT fk_room_members_room
    FOREIGN KEY (room_id) REFERENCES rooms(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_room_members_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE,

    CONSTRAINT ck_room_members_seat_no CHECK (seat_no IS NULL OR seat_no IN (1,2))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng đơn giản cho flow tạo phòng / vào phòng (Servlet + JSP)
CREATE TABLE IF NOT EXISTS room_players (
                                            id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                            room_id         BIGINT UNSIGNED NOT NULL,
                                            user_id         BIGINT UNSIGNED NOT NULL,
                                            role            ENUM('HOST','PLAYER') NOT NULL DEFAULT 'PLAYER',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_room_players_room_user (room_id, user_id),
    KEY idx_room_players_room (room_id),
    KEY idx_room_players_user (user_id),

    CONSTRAINT fk_room_players_room
    FOREIGN KEY (room_id) REFERENCES rooms(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_room_players_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_messages (
                                             id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                             room_id         BIGINT UNSIGNED NOT NULL,
                                             sender_user_id  BIGINT UNSIGNED NULL,
                                             message_type    ENUM('CHAT','SYSTEM') NOT NULL DEFAULT 'CHAT',
    message_text    VARCHAR(2000) NOT NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    KEY idx_chat_room_created (room_id, created_at),

    CONSTRAINT fk_chat_room
    FOREIGN KEY (room_id) REFERENCES rooms(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_chat_sender
    FOREIGN KEY (sender_user_id) REFERENCES users(id)
    ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------
-- 3) Games / Players / Moves
-- -----------------------------
CREATE TABLE IF NOT EXISTS games (
                                     id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                     room_id           BIGINT UNSIGNED NULL,
                                     created_by_user_id BIGINT UNSIGNED NULL,
                                     mode              ENUM('ONLINE_PVP','OFFLINE_AI','OFFLINE_LOCAL') NOT NULL,
    ranked            TINYINT(1) NOT NULL DEFAULT 0,
    board_size        TINYINT UNSIGNED NOT NULL DEFAULT 15,
    status            ENUM('CREATED','IN_PROGRESS','FINISHED','ABORTED') NOT NULL DEFAULT 'CREATED',
    result            ENUM('P1_WIN','P2_WIN','DRAW','NONE') NOT NULL DEFAULT 'NONE',
    ended_reason      ENUM('FIVE_IN_ROW','RESIGN','TIMEOUT','DISCONNECT','ABORTED','UNKNOWN') NULL,
    winner_user_id    BIGINT UNSIGNED NULL,
    started_at        DATETIME(3) NULL,
    ended_at          DATETIME(3) NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    KEY idx_games_room (room_id),
    KEY idx_games_status (status),
    KEY idx_games_winner (winner_user_id),

    CONSTRAINT fk_games_room
    FOREIGN KEY (room_id) REFERENCES rooms(id)
    ON DELETE SET NULL,

    CONSTRAINT fk_games_created_by
    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
    ON DELETE SET NULL,

    CONSTRAINT fk_games_winner
    FOREIGN KEY (winner_user_id) REFERENCES users(id)
    ON DELETE SET NULL,

    CONSTRAINT ck_games_board_size CHECK (board_size BETWEEN 5 AND 50)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS game_players (
                                            id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                            game_id       BIGINT UNSIGNED NOT NULL,
                                            player_no     TINYINT UNSIGNED NOT NULL,
                                            user_id       BIGINT UNSIGNED NULL,
                                            is_bot        TINYINT(1) NOT NULL DEFAULT 0,
    bot_level     TINYINT UNSIGNED NULL,
    symbol        ENUM('X','O') NOT NULL,
    joined_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_game_players_game_player_no (game_id, player_no),
    KEY idx_game_players_user (user_id),

    CONSTRAINT fk_game_players_game
    FOREIGN KEY (game_id) REFERENCES games(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_game_players_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE SET NULL,

    CONSTRAINT ck_game_players_player_no CHECK (player_no IN (1,2)),
    CONSTRAINT ck_game_players_bot CHECK (is_bot IN (0,1))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS game_moves (
                                          id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                          game_id     BIGINT UNSIGNED NOT NULL,
                                          move_no     INT UNSIGNED NOT NULL,
                                          player_no   TINYINT UNSIGNED NOT NULL,
                                          x           TINYINT UNSIGNED NOT NULL,
                                          y           TINYINT UNSIGNED NOT NULL,
                                          created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_game_moves_game_move_no (game_id, move_no),
    UNIQUE KEY uq_game_moves_game_xy (game_id, x, y),
    KEY idx_game_moves_game_created (game_id, created_at),

    CONSTRAINT fk_game_moves_game
    FOREIGN KEY (game_id) REFERENCES games(id)
    ON DELETE CASCADE,

    CONSTRAINT ck_game_moves_player_no CHECK (player_no IN (1,2))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------
-- 4) Ranking / Leaderboard
-- -----------------------------
CREATE TABLE IF NOT EXISTS user_stats (
                                          user_id        BIGINT UNSIGNED NOT NULL,
                                          rating         INT NOT NULL DEFAULT 1000,
                                          games_played   INT UNSIGNED NOT NULL DEFAULT 0,
                                          wins           INT UNSIGNED NOT NULL DEFAULT 0,
                                          losses         INT UNSIGNED NOT NULL DEFAULT 0,
                                          draws          INT UNSIGNED NOT NULL DEFAULT 0,
                                          win_streak     INT UNSIGNED NOT NULL DEFAULT 0,
                                          max_win_streak INT UNSIGNED NOT NULL DEFAULT 0,
                                          last_game_at   DATETIME(3) NULL,
    updated_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (user_id),
    KEY idx_user_stats_rating (rating),

    CONSTRAINT fk_user_stats_user
    FOREIGN KEY (user_id) REFERENCES users(id)
                                                                     ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Optional view: dùng để hiển thị leaderboard nhanh
CREATE OR REPLACE VIEW v_leaderboard AS
SELECT
    u.id AS user_id,
    u.username,
    COALESCE(p.display_name, u.username) AS display_name,
    s.rating,
    s.wins,
    s.losses,
    s.draws,
    s.games_played,
    s.updated_at
FROM users u
         LEFT JOIN user_profiles p ON p.user_id = u.id
         LEFT JOIN user_stats s ON s.user_id = u.id
WHERE u.status = 'ACTIVE' AND u.role = 'PLAYER';

-- -----------------------------
-- 5) Admin / System Logs
-- -----------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
                                          id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                          actor_user_id BIGINT UNSIGNED NULL,
                                          action        VARCHAR(100) NOT NULL,
    target_type   VARCHAR(50) NULL,
    target_id     BIGINT UNSIGNED NULL,
    details_json  JSON NULL,
    ip            VARCHAR(45) NULL,
    user_agent    VARCHAR(255) NULL,
    created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    KEY idx_audit_actor_created (actor_user_id, created_at),
    KEY idx_audit_action_created (action, created_at),

    CONSTRAINT fk_audit_actor
    FOREIGN KEY (actor_user_id) REFERENCES users(id)
    ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
