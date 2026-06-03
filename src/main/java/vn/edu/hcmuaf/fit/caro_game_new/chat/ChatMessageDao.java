package vn.edu.hcmuaf.fit.caro_game_new.chat;

import vn.edu.hcmuaf.fit.caro_game_new.model.ChatMessage;
import vn.edu.hcmuaf.fit.demo3.db.DbUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageDao {

    public long saveMessage(Connection connection, long roomId, long senderId, String content)
            throws SQLException {

        String sql = "INSERT INTO messages (room_id, sender_id, content) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, roomId);
            ps.setLong(2, senderId);
            ps.setString(3, content);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
                throw new SQLException("Không lấy được messageId sau khi INSERT");
            }
        }
    }

    public List<ChatMessage> loadMessages(long roomId) throws SQLException {
        String sql = """
            SELECT m.id, m.room_id, m.sender_id, u.username AS sender_username,
                   m.content, m.created_at
            FROM messages m
            JOIN users u ON u.id = m.sender_id
            WHERE m.room_id = ?
            ORDER BY m.created_at ASC
            """;

        List<ChatMessage> result = new ArrayList<>();

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, roomId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    ChatMessage msg = new ChatMessage();

                    msg.setId(rs.getLong("id"));
                    msg.setRoomId(rs.getLong("room_id"));

                    msg.setSenderUserId(rs.getLong("sender_id"));

                    msg.setMessageText(rs.getString("content"));

                    msg.setMessageType("TEXT");

                    msg.setCreatedAt(
                            rs.getObject("created_at", LocalDateTime.class)
                    );

                    result.add(msg);
                }
            }
        }

        return result;
    }
}

