
import vn.edu.hcmuaf.fit.demo3.db.DbUtil;
import vn.edu.hcmuaf.fit.demo3.model.Room;
import vn.edu.hcmuaf.fit.demo3.model.RoomPlayer;
import vn.edu.hcmuaf.fit.demo3.model.RoomStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RoomDao {
    // -	4.5: Người chơi nhập mã phòng và bấm “Vào phòng”
    public long createRoom(String roomCode, String roomName, int boardSize, long hostId, String passwordHash) throws SQLException {
        String sql = """
                INSERT INTO rooms (room_code, room_name, owner_user_id, status, is_private, room_password_hash, max_players, board_size)
                VALUES (?, ?, ?, 'OPEN', ?, ?, 2, ?)
                """;

        try (Connection c = DbUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, roomCode);
            ps.setString(2, roomName);
            ps.setLong(3, hostId);
            ps.setBoolean(4, passwordHash != null && !passwordHash.isBlank());
            ps.setString(5, passwordHash);
            ps.setInt(6, boardSize);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Không tạo được room id");
                return keys.getLong(1);
            }
        }
    }

    public void addHostToRoom(long roomId, long hostId) throws SQLException {
        String sql = """
                INSERT INTO room_players (room_id, user_id, role)
                VALUES (?, ?, 'HOST')
                """;
        try (Connection c = DbUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            ps.setLong(2, hostId);
            ps.executeUpdate();
        }
    }

    public Optional<Room> findByCode(String roomCode) throws SQLException {
        String sql = """
                SELECT id, room_code, room_name, owner_user_id, status, room_password_hash, board_size, created_at
                FROM rooms
                WHERE room_code = ?
                LIMIT 1
                """;

        try (Connection c = DbUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, roomCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRoom(rs));
            }
        }
    }

    public Optional<Room> findById(long roomId) throws SQLException {
        String sql = """
                SELECT id, room_code, room_name, owner_user_id, status, room_password_hash, board_size, created_at
                FROM rooms
                WHERE id = ?
                LIMIT 1
                """;
        try (Connection c = DbUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRoom(rs));
            }
        }
    }

    public void updateStatus(long roomId, RoomStatus status) throws SQLException {
        String sql = "UPDATE rooms SET status = ? WHERE id = ?";
        try (Connection c = DbUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.toDbValue());
            ps.setLong(2, roomId);
            ps.executeUpdate();
        }
    }

    public boolean existsByCode(String roomCode) throws SQLException {
        String sql = "SELECT 1 FROM rooms WHERE room_code = ? LIMIT 1";
        try (Connection c = DbUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, roomCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
    public List<RoomPlayer> findPlayers(long roomId) throws SQLException {
        String sql = """
                SELECT rp.id, rp.room_id, rp.user_id, rp.role, rp.created_at,
                       u.username, p.display_name
                FROM room_players rp
                JOIN users u ON u.id = rp.user_id
                LEFT JOIN user_profiles p ON p.user_id = u.id
                WHERE rp.room_id = ?
                ORDER BY CASE WHEN rp.role = 'HOST' THEN 0 ELSE 1 END, rp.created_at
                """;
        List<RoomPlayer> players = new ArrayList<>();
        try (Connection c = DbUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    players.add(new RoomPlayer(
                            rs.getLong("id"),
                            rs.getLong("room_id"),
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getString("display_name"),
                            rs.getString("role"),
                            null,
                            null,
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        }
        return players;
    }

    public List<Long> findPlayerIdsOrdered(long roomId) throws SQLException {
        String sql = """
                SELECT user_id
                FROM room_players
                WHERE room_id = ?
                ORDER BY CASE WHEN role = 'HOST' THEN 0 ELSE 1 END, created_at
                """;
        List<Long> userIds = new ArrayList<>();
        try (Connection c = DbUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getLong("user_id"));
                }
            }
        }
        return userIds;
    }
    // 4.5: Người chơi nhập mã phòng và bấm “Vào phòng”
    public JoinResult joinRoom(long roomId, long userId) throws SQLException {
        try (Connection c = DbUtil.getConnection()) {
            c.setAutoCommit(false);
            try {
                Room room = lockRoom(c, roomId);
                if (room == null) {
                    c.rollback();
                    return JoinResult.NOT_FOUND;
                }
                if (room.getStatus() != RoomStatus.WAITING) {
                    c.rollback();
                    return JoinResult.NOT_WAITING;
                }

                if (isAlreadyJoined(c, roomId, userId)) {
                    c.commit();
                    return JoinResult.JOINED;
                }

                int joined = countJoinedPlayers(c, roomId);
                if (joined >= 2) {
                    c.rollback();
                    return JoinResult.FULL;
                }

                String insertSql = """
                        INSERT INTO room_players (room_id, user_id, role)
                        VALUES (?, ?, 'PLAYER')
                        """;
                try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                    ps.setLong(1, roomId);
                    ps.setLong(2, userId);
                    ps.executeUpdate();
                }

                int newCount = countJoinedPlayers(c, roomId);
                if (newCount >= 2) {
                    try (PreparedStatement ps = c.prepareStatement("UPDATE rooms SET status = 'IN_GAME' WHERE id = ?")) {
                        ps.setLong(1, roomId);
                        ps.executeUpdate();
                    }
                }

                c.commit();
                return JoinResult.JOINED;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
        // -	4.12: Server tải dữ liệu phòng và forward sang UI phòng
        public List<RoomPlayer> findPlayers(long roomId) throws SQLException {
            String sql = """
                SELECT rp.id, rp.room_id, rp.user_id, rp.role, rp.created_at,
                       u.username, p.display_name
                FROM room_players rp
                JOIN users u ON u.id = rp.user_id
                LEFT JOIN user_profiles p ON p.user_id = u.id
                WHERE rp.room_id = ?
                ORDER BY CASE WHEN rp.role = 'HOST' THEN 0 ELSE 1 END, rp.created_at
                """;
            List<RoomPlayer> players = new ArrayList<>();
            try (Connection c = DbUtil.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, roomId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        players.add(new RoomPlayer(
                                rs.getLong("id"),
                                rs.getLong("room_id"),
                                rs.getLong("user_id"),
                                rs.getString("username"),
                                rs.getString("display_name"),
                                rs.getString("role"),
                                null,
                                null,
                                rs.getTimestamp("created_at").toLocalDateTime()
                        ));
                    }
                }
            }
            return players;
        }
    }