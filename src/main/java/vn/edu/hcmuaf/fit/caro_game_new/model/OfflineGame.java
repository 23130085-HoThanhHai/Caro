package vn.edu.hcmuaf.fit.demo3.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OfflineGame implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    public enum GameMode {
        VS_BOT, TWO_PLAYERS
    }

    private String gameId;
    private int boardSize;
    private byte[][] board;  // 0=empty, 1=player, 2=bot
    private List<int[]> moves;  // list of [x, y]
    private int currentPlayer;  // 1=player, 2=bot
    private GameState state;
    private GameResult result;
    private Difficulty difficulty;
    private GameMode gameMode;
    private long createdAt;
    private long updatedAt;

    public OfflineGame(String gameId, int boardSize, Difficulty difficulty, GameMode gameMode) {
        this.gameId = gameId;
        this.boardSize = boardSize;
        this.board = new byte[boardSize][boardSize];
        this.moves = new ArrayList<>();
        this.currentPlayer = 1;  // Player goes first
        this.state = GameState.IN_PROGRESS;
        this.result = GameResult.NONE;
        this.difficulty = difficulty;
        this.gameMode = gameMode;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public String getGameId() { return gameId; }
    public int getBoardSize() { return boardSize; }
    public byte[][] getBoard() { return board; }
    public List<int[]> getMoves() { return moves; }
    public int getCurrentPlayer() { return currentPlayer; }
    public GameState getState() { return state; }
    public GameResult getResult() { return result; }
    public Difficulty getDifficulty() { return difficulty; }
    public GameMode getGameMode() { return gameMode; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    public void setCurrentPlayer(int player) { this.currentPlayer = player; }
    public void setState(GameState state) { this.state = state; }
    public void setResult(GameResult result) { this.result = result; }
    public void setUpdatedAt(long time) { this.updatedAt = time; }

    public byte getCell(int x, int y) {
        if (x < 0 || x >= boardSize || y < 0 || y >= boardSize) return -1;
        return board[x][y];
    }

    public void setCell(int x, int y, byte value) {
        if (x >= 0 && x < boardSize && y >= 0 && y < boardSize) {
            board[x][y] = value;
        }
    }

    public void addMove(int x, int y, int player) {
        // [UC-05.1.5]: Hệ thống ghi nhận giá trị nước đi vào mảng trạng thái bàn cờ (board)
        // và lưu thông tin tọa độ vào danh sách lịch sử di chuyển (moves).
        setCell(x, y, (byte) player);
        moves.add(new int[]{x, y});
        setUpdatedAt(System.currentTimeMillis());
    }

    public boolean isCellEmpty(int x, int y) {
        return getCell(x, y) == 0;
    }
}
