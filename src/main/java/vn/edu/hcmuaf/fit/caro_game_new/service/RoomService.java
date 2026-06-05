package vn.edu.hcmuaf.fit.caro_game_new.service;

import vn.edu.hcmuaf.fit.caro_game_new.Dao.RoomDao;
import vn.edu.hcmuaf.fit.caro_game_new.model.AuthUser;
import vn.edu.hcmuaf.fit.caro_game_new.model.Room;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class RoomService {
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RoomDao roomDao = new RoomDao();

    public Room createRoom(AuthUser authUser, String roomName, int boardSize) throws SQLException, RoomException {
        requireLogin(authUser);
        if (roomName == null || roomName.isBlank()) {
            throw new RoomException("Tên phòng không được để trống");
        }
        if (roomName.length() > 100) {
            throw new RoomException("Tên phòng tối đa 100 ký tự");
        }
        if (boardSize < 5 || boardSize > 50) {
            throw new RoomException("Kích thước bàn cờ phải từ 5 đến 50");
        }

        String roomCode = generateUniqueRoomCode();
        long roomId = roomDao.createRoom(roomCode, roomName.trim(), boardSize, authUser.getId(), null);
        roomDao.addHostToRoom(roomId, authUser.getId());
        return getRoomById(roomId).orElseThrow(() -> new RoomException("Không tải được phòng vừa tạo"));
    }

    public Room joinByCode(AuthUser authUser, String roomCode) throws SQLException, RoomException {
        requireLogin(authUser);
        if (roomCode == null || roomCode.isBlank()) {
            throw new RoomException("Vui lòng nhập mã phòng");
        }

        String normalizedCode = roomCode.trim().toUpperCase();
        Room room = roomDao.findByCode(normalizedCode).orElseThrow(() -> new RoomException("Phòng không tồn tại"));

        RoomDao.JoinResult result = roomDao.joinRoom(room.getId(), authUser.getId());
        if (result == RoomDao.JoinResult.NOT_WAITING) {
            throw new RoomException("Phòng không ở trạng thái chờ");
        }
        if (result == RoomDao.JoinResult.FULL) {
            throw new RoomException("Phòng đã đầy");
        }
        if (result == RoomDao.JoinResult.NOT_FOUND) {
            throw new RoomException("Phòng không tồn tại");
        }

        return getRoomByCode(normalizedCode).orElseThrow(() -> new RoomException("Không tải được phòng"));
    }

    public Optional<Room> getRoomByCode(String roomCode) throws SQLException {
        Optional<Room> roomOpt = roomDao.findByCode(roomCode);
        if (roomOpt.isEmpty()) return Optional.empty();
        Room room = roomOpt.get();
        room.setPlayers(roomDao.findPlayers(room.getId()));
        return Optional.of(room);
    }

    public Optional<Room> getRoomById(long roomId) throws SQLException {
        Optional<Room> roomOpt = roomDao.findById(roomId);
        if (roomOpt.isEmpty()) return Optional.empty();
        Room room = roomOpt.get();
        room.setPlayers(roomDao.findPlayers(room.getId()));
        return Optional.of(room);
    }

    private String generateUniqueRoomCode() throws SQLException, RoomException {
        for (int i = 0; i < 30; i++) {
            String code = randomCode(6 + RANDOM.nextInt(3)); // 6-8 chars
            if (!roomDao.existsByCode(code)) return code;
        }
        throw new RoomException("Không thể tạo mã phòng, vui lòng thử lại");
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private void requireLogin(AuthUser authUser) throws RoomException {
        if (authUser == null) throw new RoomException("Bạn cần đăng nhập để dùng chức năng phòng");
    }
}
