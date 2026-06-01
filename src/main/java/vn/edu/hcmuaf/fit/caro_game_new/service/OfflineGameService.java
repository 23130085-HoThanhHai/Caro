package vn.edu.hcmuaf.fit.demo3.service;

import vn.edu.hcmuaf.fit.demo3.model.GameResult;
import vn.edu.hcmuaf.fit.demo3.model.GameState;
import vn.edu.hcmuaf.fit.demo3.model.OfflineGame;
import vn.edu.hcmuaf.fit.demo3.model.OfflineGame.Difficulty;
import vn.edu.hcmuaf.fit.demo3.model.OfflineGame.GameMode;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OfflineGameService {
    private static final ConcurrentHashMap<String, OfflineGame> games = new ConcurrentHashMap<>();
    private static final long GAME_TIMEOUT = 30 * 60 * 1000; // 30 min

    private OfflineGameService() {}

    public static OfflineGame createGame(int boardSize, Difficulty difficulty) {
        return createGame(boardSize, difficulty, GameMode.VS_BOT);
    }

    public static OfflineGame createGame(int boardSize, Difficulty difficulty, GameMode gameMode) {
        String gameId = UUID.randomUUID().toString();
        OfflineGame game = new OfflineGame(gameId, boardSize, difficulty, gameMode);
        games.put(gameId, game);
        return game;
    }

    public static OfflineGame createGame(int boardSize) {
        return createGame(boardSize, Difficulty.MEDIUM);
    }

    public static OfflineGame getGame(String gameId) {
        OfflineGame game = games.get(gameId);
        if (game != null) {
            long age = System.currentTimeMillis() - game.getCreatedAt();
            if (age > GAME_TIMEOUT) {
                games.remove(gameId);
                return null;
            }
        }
        return game;
    }

    public static void deleteGame(String gameId) {
        games.remove(gameId);
    }

    public static boolean playerMove(OfflineGame game, int x, int y) {
        // [UC-05.1.3]: Hệ thống kiểm tra và xác nhận ô được chọn hiện tại đang trống và đang trong lượt đi của người chơi.
        // [UC-05.5]: Ô đã có quân cờ (Invalid Move) - Hệ thống nhận diện ô đã có giá trị (khác 0) và từ chối.
        if (game == null || game.getState() != GameState.IN_PROGRESS) {
            return false;
        }
        if (!game.isCellEmpty(x, y)) {
            return false;
        }
        if (game.getCurrentPlayer() != 1) {
            return false;
        }
        // [UC-05.1.5]: Hệ thống ghi nhận giá trị nước đi vào mảng trạng thái bàn cờ (board) và lưu thông tin tọa độ vào moves.
        game.addMove(x, y, 1);

        // [UC-05.1.6]: Hệ thống thực hiện kiểm tra điều kiện kết thúc ván đấu (Thắng/Hòa).
        // [UC-05.3]: Người chơi giành chiến thắng (Win) - Hệ thống xác định có 5 quân cờ liên tiếp.
        if (GomokuRules.isWinning(game, x, y, 1)) {
            game.setState(GameState.FINISHED);
            game.setResult(GameResult.P1_WIN);
            return true;
        }

        // [UC-05.4]: Trận đấu hòa (Draw) - Hệ thống kiểm tra bàn cờ đã đầy nhưng không có ai thắng.
        if (isBoardFull(game)) {
            game.setState(GameState.FINISHED); // [UC-05.4.2]: Trạng thái game chuyển thành FINISHED.
            game.setResult(GameResult.DRAW); // [UC-05.4.2]: Kết quả là DRAW.
            return true;
        }

        // [UC-05.1.7]: Hệ thống thực hiện chuyển lượt bằng cách cập nhật thuộc tính người chơi hiện tại (currentPlayer).
        game.setCurrentPlayer(2);
        return true;
    }

    public static boolean localMove(OfflineGame game, int x, int y) {
        if (game == null || game.getState() != GameState.IN_PROGRESS) return false;
        if (!game.isCellEmpty(x, y)) return false;

        int currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != 1 && currentPlayer != 2) return false;

        game.addMove(x, y, currentPlayer);
        if (GomokuRules.isWinning(game, x, y, currentPlayer)) {
            game.setState(GameState.FINISHED);
            game.setResult(currentPlayer == 1 ? GameResult.P1_WIN : GameResult.P2_WIN);
            return true;
        }

        if (isBoardFull(game)) {
            game.setState(GameState.FINISHED);
            game.setResult(GameResult.DRAW);
            return true;
        }

        game.setCurrentPlayer(currentPlayer == 1 ? 2 : 1);
        return true;
    }

    public static int[] botMove(OfflineGame game) {
        if (game == null || game.getState() != GameState.IN_PROGRESS) {
            return null;
        }
        if (game.getCurrentPlayer() != 2) {
            return null;
        }

        // [UC-05.2.2]: Máy sử dụng thuật toán AI để tìm tọa độ tối ưu và thực hiện nước đi.
        int[] move = GomokuAI.makeMove(game);
        if (move == null) {
            game.setState(GameState.FINISHED);
            game.setResult(GameResult.DRAW);
            return null;
        }

        // [UC-05.1.5]: Ghi nhận nước đi của Máy vào board và moves.
        game.addMove(move[0], move[1], 2);

        // [UC-05.2.3]: Hệ thống kiểm tra điều kiện kết thúc cho nước đi của Máy.
        if (GomokuRules.isWinning(game, move[0], move[1], 2)) {
            game.setState(GameState.FINISHED);
            game.setResult(GameResult.P2_WIN);
            return move;
        }

        if (isBoardFull(game)) {
            game.setState(GameState.FINISHED);
            game.setResult(GameResult.DRAW);
            return move;
        }

        // [UC-05.2.4]: Hệ thống chuyển lại lượt cho Người chơi (P1).
        game.setCurrentPlayer(1);
        return move;
    }

    public static boolean resign(OfflineGame game) {
        if (game == null || game.getState() != GameState.IN_PROGRESS) {
            return false;
        }
        // [UC-05.7]: Đầu hàng (Resign).
        // [UC-05.7.2]: Hệ thống xác nhận và lập tức chuyển trạng thái sang FINISHED.
        game.setState(GameState.FINISHED);
        if (game.getGameMode() == GameMode.TWO_PLAYERS) {
            game.setResult(game.getCurrentPlayer() == 1 ? GameResult.P2_WIN : GameResult.P1_WIN);
        } else {
            game.setResult(GameResult.P2_WIN);
        }
        return true;
    }

    public static boolean undoLastRound(OfflineGame game) {
        if (game == null || game.getMoves().isEmpty()) {
            return false;
        }

        int undoCount;
        if (game.getGameMode() == GameMode.TWO_PLAYERS) {
            undoCount = 1;
        } else {
            undoCount = game.getCurrentPlayer() == 1 && game.getMoves().size() >= 2 ? 2 : 1;
        }
        for (int i = 0; i < undoCount; i++) {
            int lastIdx = game.getMoves().size() - 1;
            if (lastIdx < 0) break;
            int[] move = game.getMoves().remove(lastIdx);
            game.setCell(move[0], move[1], (byte) 0);
        }

        game.setState(GameState.IN_PROGRESS);
        game.setResult(GameResult.NONE);
        game.setCurrentPlayer(game.getMoves().size() % 2 == 0 ? 1 : 2);
        game.setUpdatedAt(System.currentTimeMillis());
        // [UC-05.6]: Hoàn tác nước đi (Undo).
        // [UC-05.6.2 & 5.6.3]: Hệ thống xóa 2 nước (đấu máy) hoặc 1 nước (đấu 2 người) và cập nhật lại trạng thái.
        // (Logic xóa moves và cập nhật currentPlayer nằm ở đây)
        return true;
    }

    private static boolean isBoardFull(OfflineGame game) {
        int boardSize = game.getBoardSize();
        for (int x = 0; x < boardSize; x++) {
            for (int y = 0; y < boardSize; y++) {
                if (game.isCellEmpty(x, y)) return false;
            }
        }
        return true;
    }
}
