import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vn.edu.hcmuaf.fit.demo3.dao.RoomGameDao;
import vn.edu.hcmuaf.fit.demo3.model.*;
import vn.edu.hcmuaf.fit.demo3.service.RoomGameService;

/**
 * Hệ thống Unit Test cho Use Case UC-05: THỰC HIỆN BƯỚC ĐI.
 * Người thực hiện test: Lê Đăng Tuấn Phát
 * Tiêu chí: Không sử dụng Mockito (Dùng cấu trúc bộ nhớ Fake tạm thời trên RAM).
 */
@DisplayName("UC-05: THỰC HIỆN BƯỚC ĐI - Người test: Lê Đăng Tuấn Phát")
class RoomGameServiceTableTest {

    private FakeRoomService fakeRoomService;
    private FakeRoomGameDao fakeRoomGameDao;
    private RoomGameService roomGameService;

    private Room mockRoom;
    private AuthUser player1;
    private AuthUser player2;
    private final long GAME_ID = 777L;

    @BeforeEach
    void setUp() {
        // Khởi tạo các lớp giả lập thủ công thay thế Mockito
        fakeRoomService = new FakeRoomService();
        fakeRoomGameDao = new FakeRoomGameDao();
        
        // Tiêm các lớp Fake vào Service thông qua Constructor nhận tham số
        roomGameService = new RoomGameService(fakeRoomService, fakeRoomGameDao);

        // Khởi tạo thông tin người chơi mẫu
        player1 = new AuthUser(101L, "Player_1");
        player2 = new AuthUser(102L, "Player_2");

        // Thiết lập cấu hình phòng thi đấu mặc định
        mockRoom = new Room();
        mockRoom.setId(5L);
        mockRoom.setRoomCode("UC05_ROOM");
        mockRoom.setBoardSize(15);
        mockRoom.setStatus(RoomStatus.PLAYING); // Tiền điều kiện chung: Phòng ở trạng thái PLAYING
        mockRoom.getPlayers().add(new RoomPlayer(101L, 1)); // Player 1 (Mã số 1)
        mockRoom.getPlayers().add(new RoomPlayer(102L, 2)); // Player 2 (Mã số 2)

        RoomGameDao.GameInfo gameInfo = new RoomGameDao.GameInfo(GAME_ID, 5L, "IN_PROGRESS", "NONE");

        // Cài đặt dữ liệu nền vào các lớp Fake
        fakeRoomService.setRoom(mockRoom);
        fakeRoomGameDao.setLatestGame(gameInfo);
    }

    @Test
    @DisplayName("TC-01: Kiểm tra xác định tọa độ khi Click")
    void test_KiMTraXacDinhToaDoKhiClick() throws SQLException, RoomException {
        // Tiền điều kiện & Các bước thực hiện: Lịch sử bàn cờ trống (moves = 0), P1 đánh vào (7,7)
        fakeRoomGameDao.setupExistingMoves(new ArrayList<>());

        // Thực hiện bước đi
        RoomGameSnapshot snapshot = roomGameService.placeMove(player1, "UC05_ROOM", 7, 7);

        // Kết quả mong đợi (Expected Output)
        assertNotNull(snapshot);
        assertTrue(fakeRoomGameDao.isAddMoveCalled, "Ghi nhận nước đi thành công vào DB (addMove gọi 1 lần)");
        assertEquals("IN_PROGRESS", snapshot.getGameStatus(), "Trạng thái ván cờ: IN_PROGRESS");
        
        // Kiểm tra xem ma trận trả về có quân số 1 tại vị trí (7,7) không
        int[][] board = snapshot.getBoard();
        assertEquals(1, board[7][7], "RoomGameSnapshot trả về ma trận có quân số 1 tại vị trí (7,7)");
    }

    @Test
    @DisplayName("TC-02: Kiểm tra tính hợp lệ của ô trống (Client) - Lỗi vượt biên")
    void test_KiMTraTinhHopLeCuaOTrong_VutBien() {
        // Tiền điều kiện & Các bước thực hiện: boardSize = 15, tọa độ x = 15, y = 7
        mockRoom.setBoardSize(15);

        // Kích hoạt hành động và kiểm tra kết quả mong đợi
        RoomException exception = assertThrows(RoomException.class, () -> {
            roomGameService.placeMove(player1, "UC05_ROOM", 15, 7);
        });

        // Kết quả mong đợi: Hệ thống từ chối và ném ra RoomException: "Tọa độ không hợp lệ"
        // Lưu ý: Tùy thuộc vào chuỗi text chính xác trong mã nguồn RoomGameService của bạn, 
        // ở đây mã gốc ném "Nước đi ngoài bàn cờ", bạn có thể tinh chỉnh lại text nếu cần.
        assertTrue(exception.getMessage().contains("ngoại") || exception.getMessage().contains("hợp lệ"));
        assertFalse(fakeRoomGameDao.isAddMoveCalled, "Không ghi bất kỳ dữ liệu nào vào Database");
    }

    @Test
    @DisplayName("TC-03: Chặn lỗi đánh trùng vào ô cờ đã bị chiếm chỗ")
    void test_ChanLoiDanhTrungVaoOCoDaBiChiemCho() {
        // Tiền điều kiện: Ô (7,7) đã có quân của Player 1 từ trước. Hiện tại đến lượt Player 2.
        List<RoomGameDao.Move> existingMoves = new ArrayList<>();
        existingMoves.add(new RoomGameDao.Move(1L, GAME_ID, 7, 7, 1, 1)); // Ô (7,7) mang quân số 1
        fakeRoomGameDao.setupExistingMoves(existingMoves);

        // Các bước thực hiện: Player 2 click vào chính ô (7,7) đã có quân
        RoomException exception = assertThrows(RoomException.class, () -> {
            roomGameService.placeMove(player2, "UC05_ROOM", 7, 7);
        });

        // Kết quả mong đợi: Hệ thống từ chối ghi nhận, ném lỗi "Ô này đã có quân"
        assertEquals("Ô này đã có quân", exception.getMessage());
        assertFalse(fakeRoomGameDao.isFinishGameCalled);
        // Biến này kiểm tra xem có phát sinh lệnh addMove mới hay không. 
        // Do chỉ có 1 move khởi tạo nền, lệnh addMove trong lượt đánh lỗi không được kích hoạt.
        assertFalse(fakeRoomGameDao.isAddMoveCalled, "Hàm addMove của DAO không được kích hoạt, bàn cờ giữ nguyên");
    }

    @Test
    @DisplayName("TC-04: Xác nhận Player 1 chiến thắng hàng ngang")
    void test_XacNhanPlayer1ChienThangHangNgang() throws SQLException, RoomException {
        // Tiền điều kiện: Player 1 đã có sẵn 4 quân liên tiếp tại hàng 0: (0,0), (1,0), (2,0), (3,0)
        // Cần xếp xen kẽ nước đi hợp lệ để tổng số moves hiện tại là Chẵn (8 moves) để tiếp theo đến lượt P1
        List<RoomGameDao.Move> moves = new ArrayList<>();
        moves.add(new RoomGameDao.Move(1L, GAME_ID, 0, 0, 1, 1)); // P1
        moves.add(new RoomGameDao.Move(2L, GAME_ID, 0, 5, 2, 2)); // P2 chặn chỗ khác
        moves.add(new RoomGameDao.Move(3L, GAME_ID, 1, 0, 1, 3)); // P1
        moves.add(new RoomGameDao.Move(4L, GAME_ID, 1, 5, 2, 4)); // P2
        moves.add(new RoomGameDao.Move(5L, GAME_ID, 2, 0, 1, 5)); // P1
        moves.add(new RoomGameDao.Move(6L, GAME_ID, 2, 5, 2, 6)); // P2
        moves.add(new RoomGameDao.Move(7L, GAME_ID, 3, 0, 1, 7)); // P1
        moves.add(new RoomGameDao.Move(8L, GAME_ID, 3, 5, 2, 8)); // P2
        fakeRoomGameDao.setupExistingMoves(moves);

        // Các bước thực hiện: Người chơi Player 1 đánh vào tọa độ x = 4, y = 0
        RoomGameSnapshot snapshot = roomGameService.placeMove(player1, "UC05_ROOM", 4, 0);

        // Kết quả mong đợi: Thuật toán phát hiện đủ 5 quân, gọi finishGame với kết quả P1_WIN
        assertNotNull(snapshot);
        assertTrue(fakeRoomGameDao.isFinishGameCalled, "Hệ thống bắt buộc phải gọi hàm finishGame(...)");
        assertEquals("P1_WIN", fakeRoomGameDao.finalResultSaved, "Kết quả trả về phải ghi nhận là P1_WIN");
        assertEquals("FINISHED", snapshot.getGameStatus(), "Trạng thái ván đấu cập nhật sang FINISHED");
    }

    @Test
    @DisplayName("TC-05: Xác nhận kết quả Hòa (Draw) khi kín bàn cờ")
    void test_XacNhanKetQuaHoaKhiKinBanCo() throws SQLException, RoomException {
        // Tiền điều kiện: Kích thước bàn cờ kiểm thử: 2x2. Đã đánh sẵn 3 nước không ai thắng.
        mockRoom.setBoardSize(2); // Đổi size sang 2x2 để tối đa 4 nước đi kí chỗ
        
        List<RoomGameDao.Move> moves = new ArrayList<>();
        moves.add(new RoomGameDao.Move(1L, GAME_ID, 0, 0, 1, 1)); // Nước 1: P1
        moves.add(new RoomGameDao.Move(2L, GAME_ID, 0, 1, 2, 2)); // Nước 2: P2
        moves.add(new RoomGameDao.Move(3L, GAME_ID, 1, 0, 1, 3)); // Nước 3: P1
        fakeRoomGameDao.setupExistingMoves(moves);

        // Các bước thực hiện: Người chơi Player 2 đánh vào ô trống cuối cùng còn lại (1, 1)
        RoomGameSnapshot snapshot = roomGameService.placeMove(player2, "UC05_ROOM", 1, 1);

        // Kết quả mong đợi: Hệ thống lưu nước đi thứ 4, nhận diện moves.size() >= boardSize * boardSize, gọi finishGame(DRAW)
        assertNotNull(snapshot);
        assertTrue(fakeRoomGameDao.isAddMoveCalled, "Hệ thống lưu nước đi thứ 4 thành công");
        assertTrue(fakeRoomGameDao.isFinishGameCalled, "Gọi hàm finishGame(...) với kết quả điều kiện kín bàn cờ");
        assertEquals("DRAW", fakeRoomGameDao.finalResultSaved, "Kết quả lưu dưới cơ sở dữ liệu phải là DRAW (Hòa)");
        assertEquals("FINISHED", snapshot.getGameStatus(), "Trạng thái ván đấu kết thúc với kết quả Hòa");
    }
}