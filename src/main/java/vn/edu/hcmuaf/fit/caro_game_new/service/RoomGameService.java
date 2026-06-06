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

/**
 * Service điều phối chính toàn bộ logic ván đấu Gomoku trực tuyến đa luồng.
 * Chịu trách nhiệm đồng bộ hóa lượt đánh, kiểm tra tính hợp lệ của nước đi và quản lý bộ nhớ trạng thái phòng.
 * * [TRỌNG TÂM NGHIỆP VỤ - UC-05: THỰC HIỆN BƯỚC ĐI]
 * Lớp này xử lý quy trình cốt lõi và các ràng buộc dữ liệu khi hệ thống tiếp nhận hành động click đặt quân cờ
 * từ phía người dùng tại Client của Use Case UC-05.
 */
public final class RoomGameService {
    
    private final RoomService roomService;
    private final RoomGameDao roomGameDao;
    
    /**
     * Bản đồ quản lý các đối tượng Lock dùng để đồng bộ đa luồng (Thread-safe) cho từng phòng đấu cụ thể.
     * [FIX]: Các phòng kết thúc ván tại UC-05 sẽ được chủ động xóa khỏi Map này để tránh lỗi rò rỉ RAM (Memory Leak).
     */
    private final ConcurrentHashMap<Long, Object> roomLocks = new ConcurrentHashMap<>();

    public RoomGameService() {
        this.roomService = new RoomService();
        this.roomGameDao = new RoomGameDao();
    }

    public RoomGameService(RoomService roomService, RoomGameDao roomGameDao) {
        this.roomService = roomService;
        this.roomGameDao = roomGameDao;
    }

    /**
     * [UC-05: THỰC HIỆN BƯỚC ĐI] - ĐỒNG BỘ TRẠNG THÁI ĐỌC
     * Lấy toàn bộ ảnh chụp trạng thái dữ liệu (Snapshot) hiện thời của ván đấu gửi về cho Client.
     * [FIX]: Đưa vào khối synchronized để ngăn tình trạng một client khác đang thực hiện UC-05 (placeMove) 
     * làm sai lệch cấu trúc dữ liệu của luồng đang đọc dữ liệu (getState).
     */
    public RoomGameSnapshot getState(AuthUser user, String roomCode) throws SQLException, RoomException {
        Room room = loadRoom(roomCode);
        
        Object lock = roomLocks.computeIfAbsent(room.getId(), k -> new Object());
        synchronized (lock) {
            ensureGameReady(room);

            RoomGameDao.GameInfo game = roomGameDao.findLatestGameByRoom(room.getId())
                    .orElseThrow(() -> new RoomException("Không tìm thấy ván trong phòng"));

            List<RoomGameDao.Move> moves = roomGameDao.findMoves(game.id());
            return buildSnapshot(room, game, moves, user.getId());
        }
    }

    /**
     * [NỘI DUNG CHÍNH CỦA UC-05: THỰC HIỆN BƯỚC ĐI]
     * Tiếp nhận yêu cầu click ô cờ, kiểm tra toàn bộ các quy tắc ràng buộc pháp lý của ván đấu trực tuyến,
     * thực hiện ghi nhận nước đi hợp lệ và cập nhật trạng thái Thắng/Thua/Hòa.
     */
    public RoomGameSnapshot placeMove(AuthUser user, String roomCode, int x, int y) throws SQLException, RoomException {
        Room room = loadRoom(roomCode);
        
        // [UC-05 - RÀNG BUỘC KIỂM TRA 1]: Xác thực tọa độ nằm trong ranh giới bàn cờ (Ví dụ: 0 đến 14 trên bàn cờ 15x15)
        if (x < 0 || y < 0 || x >= room.getBoardSize() || y >= room.getBoardSize()) {
            throw new RoomException("Nước đi ngoài bàn cờ");
        }

        Object lock = roomLocks.computeIfAbsent(room.getId(), k -> new Object());
        synchronized (lock) {
            ensureGameReady(room);
            RoomGameDao.GameInfo game = roomGameDao.findLatestGameByRoom(room.getId())
                    .orElseThrow(() -> new RoomException("Không tìm thấy ván trong phòng"));

            // [UC-05 - RÀNG BUỘC KIỂM TRA 2]: Chặn nước đi nếu trạng thái ván đấu hiện tại đã kết thúc (FINISHED)
            if ("FINISHED".equalsIgnoreCase(game.status())) {
                throw new RoomException("Ván đã kết thúc");
            }

            List<RoomGameDao.Move> moves = roomGameDao.findMoves(game.id());
            int[][] board = buildBoard(room.getBoardSize(), moves);
            
            // [UC-05 - RÀNG BUỘC KIỂM TRA 3]: Chặn lỗi đánh trùng vào vị trí ô cờ đã bị đối thủ chiếm chỗ từ trước
            if (board[y][x] != 0) throw new RoomException("Ô này đã có quân");

            int playerNo = getPlayerNo(room.getPlayers(), user.getId());
            if (playerNo == 0) throw new RoomException("Bạn không thuộc phòng này");
            
            // [UC-05 - RÀNG BUỘC KIỂM TRA 4]: Kiểm tra tính đúng lượt đánh (Tổng số nước cờ Chẵn -> Lượt P1, Lẻ -> Lượt P2)
            int expectedPlayerNo = (moves.size() % 2) + 1;
            if (playerNo != expectedPlayerNo) throw new RoomException("Chưa đến lượt của bạn");

            // [UC-05 - THỰC THI GHI DỮ LIỆU]: Thỏa mãn toàn bộ điều kiện -> Gọi DAO INSERT nước đi vào DB
            int moveNo = moves.size() + 1;
            roomGameDao.addMove(game.id(), moveNo, playerNo, x, y);
            
            // Tải lại lịch sử nước đi mới nhất sau khi vừa ghi nhận thành công nhằm phục vụ thuật toán quét kết quả UC-05
            moves = roomGameDao.findMoves(game.id());
            board = buildBoard(room.getBoardSize(), moves);

            boolean isGameOver = false;

            // [UC-05 - XỬ LÝ KẾT THÚC VÁN ĐẤU]: Ủy quyền gọi thuật toán từ GomokuRules
            if (GomokuRules.isWinning(board, x, y, playerNo)) {
                // Tình huống 5A: Phát hiện chuỗi thắng liên tiếp từ 5 quân trở lên -> Cập nhật kết quả thắng trận
                Long winnerUserId = findUserIdByPlayerNo(room.getPlayers(), playerNo);
                roomGameDao.finishGame(game.id(), playerNo == 1 ? "P1_WIN" : "P2_WIN", winnerUserId);
                game = roomGameDao.findLatestGameByRoom(room.getId()).orElse(game);
                isGameOver = true;
            } else if (moves.size() >= room.getBoardSize() * room.getBoardSize()) {
                // Tình huống 5B: Diện tích bàn cờ đã bị lấp kín hoàn toàn (moves = 225) -> Cập nhật trạng thái HÒA (DRAW)
                roomGameDao.finishGame(game.id(), "DRAW", null);
                game = roomGameDao.findLatestGameByRoom(room.getId()).orElse(game);
                isGameOver = true;
            }

            // Đóng gói đối tượng Snapshot cập nhật sau nước đi gửi về cho giao diện người dùng
            RoomGameSnapshot snapshot = buildSnapshot(room, game, moves, user.getId());

            // [FIX CHỐNG RÒ RỈ RAM TRONG UC-05]: Giải phóng Lock của phòng khỏi ConcurrentHashMap khi ván đấu ngã ngũ
            if (isGameOver) {
                roomLocks.remove(room.getId());
            }

            return snapshot;
        }
    }

    /**
     * Khởi động lại ván đấu mới (Restart Game).
     */
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

    /**
     * [UC-05: THỰC HIỆN BƯỚC ĐI] - ĐÓNG GÓI DỮ LIỆU SNAPSHOT
     * Chuyển đổi và phản hồi trạng thái mới nhất cho Client hiển thị sau nước đi của UC-05.
     */
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
        
        // [UC-05]: Cung cấp danh sách ô thắng cuộc từ GomokuRules để UI vẽ đường thẳng kết thúc ván
        if ("P1_WIN".equalsIgnoreCase(game.result()) || "P2_WIN".equalsIgnoreCase(game.result())) {
            int winnerNo = "P1_WIN".equalsIgnoreCase(game.result()) ? 1 : 2;
            snapshot.setWinningCells(GomokuRules.findWinningLine(board, winnerNo));
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
}