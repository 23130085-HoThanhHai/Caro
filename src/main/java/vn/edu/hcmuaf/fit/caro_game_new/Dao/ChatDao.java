package vn.edu.hcmuaf.fit.caro_game_new.Dao;

import vn.edu.hcmuaf.fit.caro_game_new.model.ChatMessage;
import vn.edu.hcmuaf.fit.demo3.db.DbUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatDao {

    public void saveMessage(ChatMessage message) throws SQLException {

        String sql = """
                INSERT INTO messages
                (room_id,sender_id,content,created_at)
                VALUES(?,?,?,NOW())
                """;

        try(Connection conn = DbUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setLong(1,message.getRoomId());
            ps.setLong(2,message.getSenderUserId());
            ps.setString(3,message.getMessageText());

            ps.executeUpdate();
        }
    }

    public List<ChatMessage> loadMessages(long roomId)
            throws SQLException {

        String sql = """
                SELECT m.id,
                       m.room_id,
                       m.sender_id,
                       u.username,
                       m.content,
                       m.created_at
                FROM messages m
                JOIN users u
                    ON u.id = m.sender_id
                WHERE room_id = ?
                ORDER BY created_at ASC
                """;

        List<ChatMessage> result = new ArrayList<>();

        try(Connection conn = DbUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, roomId);

            try(ResultSet rs = ps.executeQuery()) {

                while(rs.next()) {

                    ChatMessage msg = new ChatMessage();

                    msg.setId(rs.getLong("id"));
                    msg.setRoomId(rs.getLong("room_id"));
                    msg.setSenderUserId(rs.getLong("sender_id"));
                    msg.setSenderUsername(rs.getString("username"));
                    msg.setMessageText(rs.getString("content"));
                    msg.setMessageType("TEXT");

                    msg.setCreatedAt(
                            rs.getObject(
                                    "created_at",
                                    LocalDateTime.class));

                    result.add(msg);
                }
            }
        }

        return result;
    }
}

