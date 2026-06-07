package vn.edu.hcmuaf.fit.demo3.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FakeRoomGameDao extends RoomGameDao {
    
    // Lưu trữ danh sách nước đi giả lập trên RAM
    private final List<RoomGameDao.Move> ramMoves = new ArrayList<>();
    private RoomGameDao.GameInfo latestGame;
    
    // Các biến cờ (Flags) phục vụ việc xác minh Assert trong Unit Test
    public boolean isAddMoveCalled = false;
    public boolean isFinishGameCalled = false;
    public String finalResultSaved = "NONE";

    // Thiết lập trạng thái trận đấu hiện tại
    public void setLatestGame(RoomGameDao.GameInfo gameInfo) {
        this.latestGame = gameInfo;
    }

    // Nạp sẵn các nước đi nền trước khi thực hiện bấm Click test
    public void setupExistingMoves(List<RoomGameDao.Move> moves) {
        this.ramMoves.clear();
        this.ramMoves.addAll(moves);
        this.isAddMoveCalled = false; // Reset trạng thái cờ
        this.isFinishGameCalled = false;
        this.finalResultSaved = "NONE";
    }

    @Override
    public Optional<RoomGameDao.GameInfo> findLatestGameByRoom(long roomId) throws SQLException {
        return Optional.ofNullable(latestGame);
    }

    @Override
    public List<RoomGameDao.Move> findMoves(long gameId) throws SQLException {
        // Trả về danh sách các nước đi thuộc về Game ID hiện tại đang lưu trên RAM
        return ramMoves.stream().filter(m -> m.gameId() == gameId).toList();
    }

    @Override
    public void addMove(long gameId, int moveNo, int playerNo, int x, int y) throws SQLException {
        // Đánh dấu cờ xác nhận hàm addMove đã chạy thành công 1 lần
        this.isAddMoveCalled = true;
        
        // Thêm nước đi mới trực tiếp vào RAM để cập nhật bàn cờ tức thì
        long newId = ramMoves.size() + 1;
        ramMoves.add(new RoomGameDao.Move(newId, gameId, x, y, playerNo, moveNo));
    }

    @Override
    public void finishGame(long gameId, String result, Long winnerUserId) throws SQLException {
        // Đánh dấu cờ xác nhận hàm kết thúc ván đấu đã được hệ thống kích hoạt thành công
        this.isFinishGameCalled = true;
        this.finalResultSaved = result;
        
        // Cập nhật lại bản ghi ván đấu giả lập sang trạng thái FINISHED
        if (this.latestGame != null && this.latestGame.id() == gameId) {
            this.latestGame = new RoomGameDao.GameInfo(gameId, latestGame.roomId(), "FINISHED", result);
        }
    }

    @Override
    public long createGame(long roomId, long hostUserId, int boardSize) throws SQLException {
        return latestGame != null ? latestGame.id() : 1L;
    }

    @Override
    public void upsertGamePlayers(long gameId, long hostUserId, Long p2UserId) throws SQLException {
        // Không xử lý logic DB, chỉ phục vụ đi qua luồng mượt mà
    }
}