package vn.edu.hcmuaf.fit.caro_game_new.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Room {
    private long id;
    private String roomCode;
    private String name;
    private int boardSize;
    private RoomStatus status;
    private long hostId;
    private LocalDateTime createdAt;
    private List<RoomPlayer> players = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(int boardSize) {
        this.boardSize = boardSize;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<RoomPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<RoomPlayer> players) {
        this.players = players;
    }
}

