package vn.edu.hcmuaf.fit.caro_game_new.service;

import vn.edu.hcmuaf.fit.caro_game_new.Dao.ChatMessageDao;
import vn.edu.hcmuaf.fit.caro_game_new.model.AuthUser;
import vn.edu.hcmuaf.fit.caro_game_new.model.ChatMessage;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class ChatService {

    private final ChatMessageDao chatDao = new ChatMessageDao();

    public void sendMessage(
            AuthUser sender,
            long roomId,
            String text)
            throws SQLException {

        if(sender == null){
            throw new IllegalArgumentException("Chưa đăng nhập");
        }

        if(text == null || text.isBlank()){
            throw new IllegalArgumentException("Tin nhắn rỗng");
        }

        ChatMessage msg = new ChatMessage();

        msg.setRoomId(roomId);
        msg.setSenderUserId(sender.getId());
        msg.setMessageText(text.trim());
        msg.setMessageType("TEXT");
        msg.setCreatedAt(LocalDateTime.now());

        chatDao.saveMessage(msg);
    }

    public List<ChatMessage> loadMessages(long roomId)
            throws SQLException {

        return chatDao.loadMessages(roomId);
    }
}
