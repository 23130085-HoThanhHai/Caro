package vn.edu.hcmuaf.fit.caro_game_new.dao;

import vn.edu.hcmuaf.fit.demo3.db.DbUtil;
import vn.edu.hcmuaf.fit.demo3.model.GameResult;
import vn.edu.hcmuaf.fit.demo3.model.OfflineGame;
import vn.edu.hcmuaf.fit.demo3.model.OfflineGame.GameMode;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class GameHistoryDao {

    public long saveFinishedGame(OfflineGame game, Long userId) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long gameId = insertGame(conn, game, userId);
                insertGamePlayers(conn, gameId, game, userId);
                insertGameMoves(conn, gameId, game);
                conn.commit();
                return gameId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<GameRecord> findByUser(long userId, int limit) throws SQLException {
        String sql = """
                SELECT g.id, g.mode, g.board_size, g.result, g.ended_reason,
                       g.started_at, g.ended_at, g.created_at,
                       gp.player_no, gp.is_bot, gp.bot_level
                FROM games g
                JOIN game_players gp ON gp.game_id = g.id AND gp.user_id = ?
                WHERE g.mode IN ('OFFLINE_AI', 'OFFLINE_LOCAL')
                ORDER BY g.created_at DESC
                LIMIT ?
                """;
        List<GameRecord> records = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, Math.max(1, Math.min(limit, 100)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) records.add(mapRecord(rs));
            }
        }
        return records;
    }

    public List<MoveRecord> findMoves(long gameId) throws SQLException {
        String sql = """
                SELECT move_no, player_no, x, y, created_at
                FROM game_moves
                WHERE game_id = ?
                ORDER BY move_no ASC
                """;
        List<MoveRecord> moves = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    moves.add(new MoveRecord(
                            rs.getInt("move_no"),
                            rs.getInt("player_no"),
                            rs.getInt("x"),
                            rs.getInt("y"),
                            toLocalDateTime(rs.getTimestamp("created_at"))
                    ));
                }
            }
        }
        return moves;
    }

    public int countByUser(long userId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM games g
                JOIN game_players gp ON gp.game_id = g.id AND gp.user_id = ?
                WHERE g.mode IN ('OFFLINE_AI', 'OFFLINE_LOCAL')
                """;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }


    private long insertGame(Connection conn, OfflineGame game, Long userId) throws SQLException {
        String sql = """
                INSERT INTO games
                    (room_id, created_by_user_id, mode, ranked, board_size,
                     status, result, ended_reason, winner_user_id, started_at, ended_at)
                VALUES (NULL, ?, ?, 0, ?, 'FINISHED', ?, ?, NULL, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, userId);
            ps.setString(2, toDbMode(game.getGameMode()));
            ps.setInt(3, game.getBoardSize());
            ps.setString(4, toDbResult(game.getResult()));
            ps.setString(5, inferEndedReason(game));
            ps.setTimestamp(6, new Timestamp(game.getCreatedAt()));
            ps.setTimestamp(7, new Timestamp(game.getUpdatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Không lấy được game_id sau INSERT games");
                return keys.getLong(1);
            }
        }
    }

    private void insertGamePlayers(Connection conn, long gameId, OfflineGame game, Long userId) throws SQLException {
        String sql = """
                INSERT INTO game_players (game_id, player_no, user_id, is_bot, bot_level, symbol)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        boolean isVsBot = game.getGameMode() == GameMode.VS_BOT;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // Player 1 — người chơi thật
            ps.setLong(1, gameId);
            ps.setInt(2, 1);
            ps.setObject(3, userId);
            ps.setBoolean(4, false);
            ps.setNull(5, Types.TINYINT);
            ps.setString(6, "X");
            ps.addBatch();

            ps.setLong(1, gameId);
            ps.setInt(2, 2);
            ps.setNull(3, Types.BIGINT);
            ps.setBoolean(4, isVsBot);
            if (isVsBot) {
                ps.setInt(5, toBotLevel(game.getDifficulty()));
            } else {
                ps.setNull(5, Types.TINYINT);
            }
            ps.setString(6, "O");
            ps.addBatch();

            ps.executeBatch();
        }
    }

    private void insertGameMoves(Connection conn, long gameId, OfflineGame game) throws SQLException {
        if (game.getMoves().isEmpty()) return;
        String sql = """
                INSERT INTO game_moves (game_id, move_no, player_no, x, y)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            List<int[]> moves = game.getMoves();
            for (int i = 0; i < moves.size(); i++) {
                int[] move = moves.get(i);
                ps.setLong(1, gameId);
                ps.setInt(2, i + 1);
                ps.setInt(3, (i % 2 == 0) ? 1 : 2);
                ps.setInt(4, move[0]);
                ps.setInt(5, move[1]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
    private String toDbMode(GameMode mode) {
        return switch (mode) {
            case VS_BOT      -> "OFFLINE_AI";
            case TWO_PLAYERS -> "OFFLINE_LOCAL";
        };
    }

    private String toDbResult(GameResult result) {
        if (result == null) return "NONE";
        return switch (result) {
            case P1_WIN -> "P1_WIN";
            case P2_WIN -> "P2_WIN";
            case DRAW   -> "DRAW";
            default     -> "NONE";
        };
    }
    private String inferEndedReason(OfflineGame game) {
        return game.getResult() == GameResult.DRAW ? "UNKNOWN" : "FIVE_IN_ROW";
    }
    private int toBotLevel(OfflineGame.Difficulty difficulty) {
        if (difficulty == null) return 2;
        return switch (difficulty) {
            case EASY   -> 1;
            case MEDIUM -> 2;
            case HARD   -> 3;
        };
    }
    private LocalDateTime toLocalDateTime(Timestamp ts) {
        if (ts == null) return null;
        return ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    private GameRecord mapRecord(ResultSet rs) throws SQLException {
        return new GameRecord(
                rs.getLong("id"),
                rs.getString("mode"),
                rs.getInt("board_size"),
                rs.getString("result"),
                rs.getString("ended_reason"),
                rs.getInt("player_no"),
                rs.getBoolean("is_bot"),
                rs.getObject("bot_level") != null ? rs.getInt("bot_level") : null,
                toLocalDateTime(rs.getTimestamp("started_at")),
                toLocalDateTime(rs.getTimestamp("ended_at")),
                toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }
    public record GameRecord(
            long gameId,
            String mode,
            int boardSize,
            String result,
            String endedReason,
            int playerNo,
            boolean isBot,
            Integer botLevel,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            LocalDateTime createdAt
    ) {
        public boolean isWin() {
            return (playerNo == 1 && "P1_WIN".equals(result))
                || (playerNo == 2 && "P2_WIN".equals(result));
        }
        public boolean isDraw() {
            return "DRAW".equals(result);
        }
    }
    public record MoveRecord(
            int moveNo,
            int playerNo,
            int x,
            int y,
            LocalDateTime createdAt
    ) {}
}
