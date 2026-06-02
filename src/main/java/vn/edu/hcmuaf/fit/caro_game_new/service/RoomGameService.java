package vn.edu.hcmuaf.fit.demo3.service;

import vn.edu.hcmuaf.fit.demo3.dao.RoomGameDao;
import vn.edu.hcmuaf.fit.demo3.model.AuthUser;
import vn.edu.hcmuaf.fit.demo3.model.Room;
import vn.edu.hcmuaf.fit.demo3.model.RoomGameSnapshot;
import vn.edu.hcmuaf.fit.demo3.model.RoomPlayer;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RoomGameService {
    private final RoomService roomService = new RoomService();
    private final RoomGameDao roomGameDao = new RoomGameDao();
    private final ConcurrentHashMap<Long, Object> roomLocks = new ConcurrentHashMap<>();

    public RoomGameSnapshot getState(AuthUser user, String roomCode) throws SQLException, RoomException {
        Room room = loadRoom(roomCode);
        ensureGameReady(room);

        RoomGameDao.GameInfo game = roomGameDao.findLatestGameByRoom(room.getId())
                .orElseThrow(() -> new RoomException("Không tìm thấy ván trong phòng"));

        List<RoomGameDao.Move> moves = roomGameDao.findMoves(game.id());
        return buildSnapshot(room, game, moves, user.getId());
    }

    public RoomGameSnapshot placeMove(AuthUser user, String roomCode, int x, int y) throws SQLException, RoomException {
        Room room = loadRoom(roomCode);
        if (x < 0 || y < 0 || x >= room.getBoardSize() || y >= room.getBoardSize()) {
            throw new RoomException("Nước đi ngoài bàn cờ");
        }

        Object lock = roomLocks.computeIfAbsent(room.getId(), k -> new Object());
        synchronized (lock) {
            ensureGameReady(room);
            RoomGameDao.GameInfo game = roomGameDao.findLatestGameByRoom(room.getId())
                    .orElseThrow(() -> new RoomException("Không tìm thấy ván trong phòng"));

            if ("FINISHED".equalsIgnoreCase(game.status())) {
                throw new RoomException("Ván đã kết thúc");
            }

            List<RoomGameDao.Move> moves = roomGameDao.findMoves(game.id());
            int[][] board = buildBoard(room.getBoardSize(), moves);
            if (board[y][x] != 0) throw new RoomException("Ô này đã có quân");

            int playerNo = getPlayerNo(room.getPlayers(), user.getId());
            if (playerNo == 0) throw new RoomException("Bạn không thuộc phòng này");
            int expectedPlayerNo = (moves.size() % 2) + 1;
            if (playerNo != expectedPlayerNo) throw new RoomException("Chưa đến lượt của bạn");

            int moveNo = moves.size() + 1;
            roomGameDao.addMove(game.id(), moveNo, playerNo, x, y);
            moves = roomGameDao.findMoves(game.id());
            board = buildBoard(room.getBoardSize(), moves);

            if (isWinning(board, x, y, playerNo)) {
                Long winnerUserId = findUserIdByPlayerNo(room.getPlayers(), playerNo);
                roomGameDao.finishGame(game.id(), playerNo == 1 ? "P1_WIN" : "P2_WIN", winnerUserId);
                game = roomGameDao.findLatestGameByRoom(room.getId()).orElse(game);
            } else if (moves.size() >= room.getBoardSize() * room.getBoardSize()) {
                roomGameDao.finishGame(game.id(), "DRAW", null);
                game = roomGameDao.findLatestGameByRoom(room.getId()).orElse(game);
            }

            return buildSnapshot(room, game, moves, user.getId());
        }
    }

    public RoomGameSnapshot restartGame(AuthUser user, String roomCode) throws SQLException, RoomException {
        Room room = loadRoom(roomCode);
        int playerNo = getPlayerNo(room.getPlayers(), user.getId());
        if (playerNo == 0) throw new RoomException("Bạn không thuộc phòng này");
        if (room.getPlayers().size() < 2) throw new RoomException("Cần đủ 2 người để chơi lại");

        Object lock = roomLocks.computeIfAbsent(room.getId(), k -> new Object());
        synchronized (lock) {
            RoomPlayer host = room.getPlayers().get(0);
            Long p2UserId = room.getPlayers().get(1).userId();
            long newGameId = roomGameDao.createGame(room.getId(), host.userId(), room.getBoardSize());
            roomGameDao.upsertGamePlayers(newGameId, host.userId(), p2UserId);
            RoomGameDao.GameInfo game = roomGameDao.findLatestGameByRoom(room.getId())
                    .orElseThrow(() -> new RoomException("Không tạo được ván mới"));
            List<RoomGameDao.Move> moves = roomGameDao.findMoves(game.id());
            return buildSnapshot(room, game, moves, user.getId());
        }
    }

    private Room loadRoom(String roomCode) throws SQLException, RoomException {
        if (roomCode == null || roomCode.isBlank()) throw new RoomException("Thiếu mã phòng");
        Optional<Room> roomOpt = roomService.getRoomByCode(roomCode.trim().toUpperCase());
        if (roomOpt.isEmpty()) throw new RoomException("Phòng không tồn tại");
        return roomOpt.get();
    }

    private void ensureGameReady(Room room) throws SQLException {
        if (room.getPlayers().isEmpty()) return;
        RoomPlayer host = room.getPlayers().get(0);
        Long p2UserId = room.getPlayers().size() > 1 ? room.getPlayers().get(1).userId() : null;

        RoomGameDao.GameInfo game = roomGameDao.findLatestGameByRoom(room.getId()).orElse(null);
        long gameId;
        if (game == null) {
            gameId = roomGameDao.createGame(room.getId(), host.userId(), room.getBoardSize());
        } else {
            gameId = game.id();
        }
        roomGameDao.upsertGamePlayers(gameId, host.userId(), p2UserId);
    }

    private RoomGameSnapshot buildSnapshot(Room room, RoomGameDao.GameInfo game, List<RoomGameDao.Move> moves, long currentUserId) throws RoomException {
        RoomGameSnapshot snapshot = new RoomGameSnapshot();
        snapshot.setRoomId(room.getId());
        snapshot.setRoomCode(room.getRoomCode());
        snapshot.setBoardSize(room.getBoardSize());
        snapshot.setGameStatus(game.status());
        snapshot.setResult(game.result());
        snapshot.setPlayersJoined(room.getPlayers().size());
        snapshot.setCurrentPlayerNo("FINISHED".equalsIgnoreCase(game.status()) ? 0 : (moves.size() % 2) + 1);
        snapshot.setYourPlayerNo(getPlayerNo(room.getPlayers(), currentUserId));
        snapshot.setMoves(moves.stream().map(m -> new int[]{m.x(), m.y(), m.playerNo()}).toList());
        int[][] board = buildBoard(room.getBoardSize(), moves);
        snapshot.setBoard(board);
        if (room.getPlayers().size() < 2) {
            snapshot.setMessage("Đang chờ người chơi thứ 2 tham gia");
        }
        if ("P1_WIN".equalsIgnoreCase(game.result()) || "P2_WIN".equalsIgnoreCase(game.result())) {
            int winnerNo = "P1_WIN".equalsIgnoreCase(game.result()) ? 1 : 2;
            snapshot.setWinningCells(findWinningLine(board, winnerNo));
        }
        return snapshot;
    }

    private int[][] buildBoard(int boardSize, List<RoomGameDao.Move> moves) {
        int[][] board = new int[boardSize][boardSize];
        for (RoomGameDao.Move move : moves) {
            board[move.y()][move.x()] = move.playerNo();
        }
        return board;
    }

    private int getPlayerNo(List<RoomPlayer> players, long userId) {
        if (players.isEmpty()) return 0;
        if (players.get(0).userId() == userId) return 1;
        if (players.size() > 1 && players.get(1).userId() == userId) return 2;
        return 0;
    }

    private Long findUserIdByPlayerNo(List<RoomPlayer> players, int playerNo) {
        if (playerNo == 1 && !players.isEmpty()) return players.get(0).userId();
        if (playerNo == 2 && players.size() > 1) return players.get(1).userId();
        return null;
    }

    private boolean isWinning(int[][] board, int x, int y, int playerNo) {
        return count(board, x, y, playerNo, 1, 0) + count(board, x, y, playerNo, -1, 0) + 1 >= 5
                || count(board, x, y, playerNo, 0, 1) + count(board, x, y, playerNo, 0, -1) + 1 >= 5
                || count(board, x, y, playerNo, 1, 1) + count(board, x, y, playerNo, -1, -1) + 1 >= 5
                || count(board, x, y, playerNo, 1, -1) + count(board, x, y, playerNo, -1, 1) + 1 >= 5;
    }

    private int count(int[][] board, int x, int y, int p, int dx, int dy) {
        int n = board.length;
        int c = 0;
        int nx = x + dx;
        int ny = y + dy;
        while (nx >= 0 && ny >= 0 && nx < n && ny < n && board[ny][nx] == p) {
            c++;
            nx += dx;
            ny += dy;
        }
        return c;
    }

    private List<int[]> findWinningLine(int[][] board, int playerNo) {
        int n = board.length;
        int[][] dirs = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (board[y][x] != playerNo) continue;
                for (int[] d : dirs) {
                    List<int[]> line = collectLine(board, x, y, playerNo, d[0], d[1]);
                    if (line.size() >= 5) return line.subList(0, 5);
                }
            }
        }
        return List.of();
    }

    private List<int[]> collectLine(int[][] board, int x, int y, int playerNo, int dx, int dy) {
        int n = board.length;
        List<int[]> line = new java.util.ArrayList<>();
        int nx = x;
        int ny = y;
        while (nx >= 0 && ny >= 0 && nx < n && ny < n && board[ny][nx] == playerNo) {
            line.add(new int[]{nx, ny});
            nx += dx;
            ny += dy;
        }
        return line;
    }
}
