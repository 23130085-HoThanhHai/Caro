import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class RoomDaoFindPlayersTest {

    private RoomDao roomDao;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private MockedStatic<DbUtil> mockedDbUtil;

    @BeforeEach
    public void setUp() throws SQLException {
        roomDao = new RoomDao();

        // 1. Mock các đối tượng của JDBC
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        // 2. Mock static DbUtil để trả về connection giả
        mockedDbUtil = mockStatic(DbUtil.class);
        mockedDbUtil.when(DbUtil::getConnection).thenReturn(connection);

        // 3. Giả lập luồng gọi prepareStatement luôn trả về statement mock
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @AfterEach
    public void tearDown() {
        // Bắt buộc đóng mock static sau mỗi test case
        mockedDbUtil.close();
    }

    /**
     * THÔNG TIN TEST CASE 1:
     * - Phương thức test: findPlayers()
     * - Thuộc bước: 4.12 (Tải dữ liệu phòng - Nhánh: Phòng rỗng hoặc mã phòng không hợp lệ)
     * - Dữ liệu (Input):
     * + roomId = 999L
     * + ResultSet.next() trả về false ngay từ đầu (không có dòng dữ liệu nào).
     * - Kết quả mong đợi (Output):
     * + Hàm trả về một danh sách (List) rỗng.
     * + Không có Exception nào bị ném ra.
     */
    @Test
    public void testFindPlayers_Step4_12_EmptyRoom_ShouldReturnEmptyList() throws SQLException {
        // --- 1. ARRANGE ---
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // Giả lập Database không trả về dòng nào
        when(resultSet.next()).thenReturn(false);

        // --- 2. ACT ---
        List<RoomPlayer> players = roomDao.findPlayers(999L);

        // --- 3. ASSERT ---
        // Đảm bảo PreparedStatement đã gán đúng roomId vào câu query
        verify(preparedStatement, times(1)).setLong(1, 999L);
        // Danh sách trả về phải rỗng, thay vì null
        assertTrue(players.isEmpty());
    }

    /**
     * THÔNG TIN TEST CASE 2:
     * - Phương thức test: findPlayers()
     * - Thuộc bước: 4.12 (Tải dữ liệu phòng - Nhánh: Trải nghiệm hoàn hảo)
     * - Dữ liệu (Input):
     * + roomId = 1L
     * + ResultSet.next() trả về true 2 lần (đại diện cho 2 người chơi: Host và Player).
     * + Các hàm rs.get... trả về dữ liệu tương ứng cho từng dòng.
     * - Kết quả mong đợi (Output):
     * + Trả về danh sách chứa đúng 2 object RoomPlayer.
     * + Các thuộc tính được map chính xác từ ResultSet sang Java Object.
     */
    @Test
    public void testFindPlayers_Step4_12_RoomHasPlayers_ShouldReturnMappedList() throws SQLException {
        // --- 1. ARRANGE ---
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Giả lập ResultSet có 2 dòng dữ liệu, đến dòng thứ 3 thì hết (false)
        when(resultSet.next()).thenReturn(true, true, false);

        // Giả lập dữ liệu trả về cho 2 dòng bằng cách truyền nhiều tham số vào thenReturn
        when(resultSet.getLong("id")).thenReturn(101L, 102L);
        when(resultSet.getLong("room_id")).thenReturn(1L, 1L);
        when(resultSet.getLong("user_id")).thenReturn(10L, 20L);

        when(resultSet.getString("username")).thenReturn("chu_phong", "khach_vao_choi");
        when(resultSet.getString("display_name")).thenReturn("Chủ Phòng Caro", "Khách Vô Danh");
        when(resultSet.getString("role")).thenReturn("HOST", "PLAYER");

        // Giả lập Timestamp
        Timestamp mockTime = Timestamp.valueOf(LocalDateTime.of(2026, 5, 20, 10, 0));
        when(resultSet.getTimestamp("created_at")).thenReturn(mockTime, mockTime);

        // --- 2. ACT ---
        List<RoomPlayer> players = roomDao.findPlayers(1L);

        // --- 3. ASSERT ---
        assertEquals(2, players.size(), "Phải lấy ra được đúng 2 người chơi");

        // Kiểm tra chi tiết người chơi thứ nhất (HOST)
        RoomPlayer host = players.get(0);
        assertEquals(101L, host.getId());
        assertEquals("chu_phong", host.getUsername());
        assertEquals("HOST", host.getRole());

        // Kiểm tra chi tiết người chơi thứ hai (PLAYER)
        RoomPlayer guest = players.get(1);
        assertEquals(102L, guest.getId());
        assertEquals("Khách Vô Danh", guest.getDisplayName());
        assertEquals("PLAYER", guest.getRole());
    }

    /**
     * THÔNG TIN TEST CASE 3:
     * - Phương thức test: findPlayers()
     * - Thuộc bước: Lỗi hệ thống khi tải dữ liệu
     * - Dữ liệu (Input):
     * + executeQuery() ném ra SQLException (ví dụ: mất mạng, lỗi cú pháp SQL).
     * - Kết quả mong đợi (Output):
     * + Phương thức không được catch mà phải ném ngược (throw) Exception đó lên cho Service/Controller xử lý.
     */
    @Test
    public void testFindPlayers_SQLException_ShouldThrowToCaller() throws SQLException {
        // --- 1. ARRANGE ---
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("Lỗi mất kết nối CSDL"));

        // --- 2. ACT & ASSERT ---
        SQLException exception = assertThrows(SQLException.class, () -> {
            roomDao.findPlayers(1L);
        });

        assertEquals("Lỗi mất kết nối CSDL", exception.getMessage());
    }
}