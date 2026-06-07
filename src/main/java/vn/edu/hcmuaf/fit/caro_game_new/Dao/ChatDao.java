package vn.edu.hcmuaf.fit.caro_game_new.Dao;

import vn.edu.hcmuaf.fit.caro_game_new.model.ChatMessage;
import vn.edu.hcmuaf.fit.caro_game_new.model.MessageType;
import vn.edu.hcmuaf.fit.demo3.db.DbUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatDao {

    public void saveMessage(ChatMessage message)
            throws SQLException {

        String sql = """
                INSERT INTO chat_messages
                (
                    room_id,
                    sender_user_id,
                    message_type,
                    message_text
                )
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(
                    1,
                    message.getRoomId()
            );

            if (message.getSenderUserId() != null) {
                ps.setLong(
                        2,
                        message.getSenderUserId()
                );
            } else {
                ps.setNull(
                        2,
                        Types.BIGINT
                );
            }

            ps.setString(
                    3,
                    message.getMessageType().toDbValue()
            );

            ps.setString(
                    4,
                    message.getMessageText()
            );

            ps.executeUpdate();
        }
    }

    public List<ChatMessage> loadMessages(long roomId)
            throws SQLException {

        String sql = """
                SELECT
                    cm.id,
                    cm.room_id,
                    cm.sender_user_id,
                    u.username,
                    cm.message_type,
                    cm.message_text,
                    cm.created_at
                FROM chat_messages cm
                LEFT JOIN users u
                    ON u.id = cm.sender_user_id
                WHERE cm.room_id = ?
                ORDER BY cm.created_at ASC
                """;

        List<ChatMessage> result = new ArrayList<>();

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, roomId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    ChatMessage msg = new ChatMessage();

                    msg.setId(
                            rs.getLong("id")
                    );

                    msg.setRoomId(
                            rs.getLong("room_id")
                    );

                    long senderId =
                            rs.getLong("sender_user_id");

                    if (!rs.wasNull()) {
                        msg.setSenderUserId(senderId);
                    }

                    msg.setSenderUsername(
                            rs.getString("username")
                    );

                    msg.setMessageType(
                            MessageType.fromDbValue(
                                    rs.getString("message_type")
                            )
                    );

                    msg.setMessageText(
                            rs.getString("message_text")
                    );

                    msg.setCreatedAt(
                            rs.getObject(
                                    "created_at",
                                    LocalDateTime.class
                            )
                    );

                    result.add(msg);
                }
            }
        }

        return result;
    }
}