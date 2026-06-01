package vn.edu.hcmuaf.fit.caro_game_new.model;

import java.time.LocalDateTime;

public class ChatMessage {
    private Long id;
    private Long roomId;
    private Long senderUserId;
    private String messageType;
    private String messageText;
    private LocalDateTime createdAt;

    public ChatMessage() {
    }

    public ChatMessage(Long id, Long roomId, Long senderUserId,
                       String messageType, String messageText,
                       LocalDateTime createdAt) {
        this.id = id;
        this.roomId = roomId;
        this.senderUserId = senderUserId;
        this.messageType = messageType;
        this.messageText = messageText;
        this.createdAt = createdAt;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(Long senderUserId) {
        this.senderUserId = senderUserId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
