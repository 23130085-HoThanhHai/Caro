import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class RoomServiceTest {

    private RoomService roomService;
    private Connection connection;
    private MockedStatic<DbUtil> mockedDbUtil;

    @BeforeEach
    public void setUp() {
        // Mock DB Connection
        connection = mock(Connection.class);

        // Mock phương thức static DbUtil.getConnection()
        mockedDbUtil = mockStatic(DbUtil.class);
        mockedDbUtil.when(DbUtil::getConnection).thenReturn(connection);

        // Spy chính class chứa phương thức joinRoom để mock các hàm nội bộ
        roomService = spy(new RoomService());
    }

    @AfterEach
    public void tearDown() {
        // Bắt buộc phải đóng MockedStatic sau mỗi test case để không ảnh hưởng test khác
        mockedDbUtil.close();
    }

    /**
     * THÔNG TIN TEST CASE 1:
     * - Phương thức test: joinRoom(long, long)
     * - Thuộc bước: 4.5 (Người chơi nhập mã phòng và bấm “Vào phòng” - Nhánh: Phòng không tồn tại)
     * - Dữ liệu (Input):
     * + roomId = 1, userId = 100
     * + lockRoom() trả về null (không tìm thấy phòng)
     * - Kết quả mong đợi (Output):
     * + Trả về JoinResult.NOT_FOUND
     * + Giao dịch DB bị hủy (c.rollback() được gọi)
     */
    @Test
    public void testJoinRoom_Step4_5_RoomNotFound_ShouldReturnNotFound() throws SQLException {
        // --- 1. ARRANGE ---
        long roomId = 1L;
        long userId = 100L;
        doReturn(null).when(roomService).lockRoom(connection, roomId);

        // --- 2. ACT ---
        JoinResult result = roomService.joinRoom(roomId, userId);

        // --- 3. ASSERT ---
        assertEquals(JoinResult.NOT_FOUND, result);
        verify(connection, times(1)).rollback();
        verify(connection, never()).commit();
    }

    /**
     * THÔNG TIN TEST CASE 2:
     * - Phương thức test: joinRoom(long, long)
     * - Thuộc bước: 4.5 (Nhánh: Trạng thái phòng không hợp lệ - đang chơi hoặc đã kết thúc)
     * - Dữ liệu (Input):
     * + Trạng thái phòng là IN_GAME (không phải WAITING)
     * - Kết quả mong đợi (Output):
     * + Trả về JoinResult.NOT_WAITING
     * + DB Rollback được gọi
     */
    @Test
    public void testJoinRoom_Step4_5_RoomNotInWaitingStatus_ShouldReturnNotWaiting() throws SQLException {
        // --- 1. ARRANGE ---
        long roomId = 1L;
        long userId = 100L;

        Room mockRoom = new Room();
        mockRoom.setStatus(RoomStatus.IN_GAME); // Giả lập phòng cờ Caro đang diễn ra

        doReturn(mockRoom).when(roomService).lockRoom(connection, roomId);

        // --- 2. ACT ---
        JoinResult result = roomService.joinRoom(roomId, userId);

        // --- 3. ASSERT ---
        assertEquals(JoinResult.NOT_WAITING, result);
        verify(connection, times(1)).rollback();
    }

    /**
     * THÔNG TIN TEST CASE 3:
     * - Phương thức test: joinRoom(long, long)
     * - Thuộc bước: 4.5 (Nhánh: Phòng đã đủ 2 người chơi)
     * - Dữ liệu (Input):
     * + Trạng thái phòng là WAITING. Người chơi chưa join trước đó.
     * + countJoinedPlayers() trả về 2 (Phòng đã full người).
     * - Kết quả mong đợi (Output):
     * + Trả về JoinResult.FULL
     * + DB Rollback bảo vệ dữ liệu, không cho insert thêm.
     */
    @Test
    public void testJoinRoom_Step4_5_RoomIsFull_ShouldReturnFull() throws SQLException {
        // --- 1. ARRANGE ---
        long roomId = 1L;
        long userId = 100L;

        Room mockRoom = new Room();
        mockRoom.setStatus(RoomStatus.WAITING);
        doReturn(mockRoom).when(roomService).lockRoom(connection, roomId);
        doReturn(false).when(roomService).isAlreadyJoined(connection, roomId, userId);

        // Giả lập phòng đã có 2 người chơi
        doReturn(2).when(roomService).countJoinedPlayers(connection, roomId);

        // --- 2. ACT ---
        JoinResult result = roomService.joinRoom(roomId, userId);

        // --- 3. ASSERT ---
        assertEquals(JoinResult.FULL, result);
        verify(connection, times(1)).rollback();
        verify(connection, never()).commit();
    }

    /**
     * THÔNG TIN TEST CASE 4:
     * - Phương thức test: joinRoom(long, long)
     * - Thuộc bước: 4.5 (Nhánh: Trải nghiệm hoàn hảo - Người chơi thứ 2 join phòng thành công)
     * - Dữ liệu (Input):
     * + Phòng WAITING. Người này chưa join.
     * + Ban đầu phòng có 1 người (countJoinedPlayers = 1).
     * + Sau khi insert, phòng có 2 người (countJoinedPlayers lần 2 = 2).
     * - Kết quả mong đợi (Output):
     * + Chạy lệnh INSERT vào bảng room_players.
     * + Chạy lệnh UPDATE trạng thái phòng thành IN_GAME.
     * + DB Commit thành công (c.commit() được gọi).
     * + Trả về JoinResult.JOINED
     */
    @Test
    public void testJoinRoom_Step4_5_SuccessAndChangeStatus_ShouldReturnJoined() throws SQLException {
        // --- 1. ARRANGE ---
        long roomId = 1L;
        long userId = 100L;
        PreparedStatement mockPs = mock(PreparedStatement.class);

        Room mockRoom = new Room();
        mockRoom.setStatus(RoomStatus.WAITING);

        doReturn(mockRoom).when(roomService).lockRoom(connection, roomId);
        doReturn(false).when(roomService).isAlreadyJoined(connection, roomId, userId);

        // Lần gọi countJoinedPlayers thứ 1 (trước khi join) trả về 1 người.
        // Lần gọi thứ 2 (sau khi insert db) trả về 2 người.
        doReturn(1).doReturn(2).when(roomService).countJoinedPlayers(connection, roomId);

        // Giả lập cho lệnh prepareStatement luôn trả về mockPs để tránh lỗi NullPointer
        when(connection.prepareStatement(anyString())).thenReturn(mockPs);

        // --- 2. ACT ---
        JoinResult result = roomService.joinRoom(roomId, userId);

        // --- 3. ASSERT ---
        assertEquals(JoinResult.JOINED, result);

        // Xác minh PreparedStatement đã chạy hàm executeUpdate() (1 cho Insert, 1 cho Update Status = 2 lần)
        verify(mockPs, times(2)).executeUpdate();

        // Đảm bảo dữ liệu được lưu vĩnh viễn vào DB
        verify(connection, times(1)).commit();

        // Kiểm tra xem connection có được trả về trạng thái AutoCommit(true) trong block finally không
        verify(connection, times(1)).setAutoCommit(true);
    }
}