import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class RoomDaoTest {

    private RoomDao roomDao;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private MockedStatic<DbUtil> mockedDbUtil;

    @BeforeEach
    public void setUp() {
        roomDao = new RoomDao();

        // Mock các thành phần của JDBC
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        // Giả lập DbUtil.getConnection() trả về connection mock
        mockedDbUtil = mockStatic(DbUtil.class);
        mockedDbUtil.when(DbUtil::getConnection).thenReturn(connection);
    }

    @AfterEach
    public void tearDown() {
        // Đóng mock static để dọn dẹp bộ nhớ sau mỗi test
        mockedDbUtil.close();
    }

    /**
     * THÔNG TIN TEST CASE 1:
     * - Phương thức test: createRoom()
     * - Thuộc logic: 4.5: Người chơi nhập mã phòng và bấm “Vào phòng”, Tạo phòng (không có mật khẩu)
     * - Dữ liệu (Input):
     * + passwordHash = "" (chuỗi rỗng), các tham số khác hợp lệ.
     * + ResultSet giả lập trả về ID = 100L.
     * - Kết quả mong đợi (Output):
     * + Hàm trả về ID là 100L.
     * + Biến isPrivate (tham số số 4) phải được set là FALSE.
     */
    @Test
    public void testCreateRoom_PublicRoom_ShouldReturnGeneratedId() throws SQLException {
        // --- 1. ARRANGE ---
        long expectedRoomId = 100L;
        String passwordHash = ""; // Phòng public

        // Phải mock chính xác hàm prepareStatement có tham số Statement.RETURN_GENERATED_KEYS
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);

        // Giả lập ResultSet có chứa dữ liệu (khóa chính được tự tạo)
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(expectedRoomId);

        // --- 2. ACT ---
        long actualRoomId = roomDao.createRoom("ROOM_123", "Phòng của tôi", 15, 1L, passwordHash);

        // --- 3. ASSERT ---
        assertEquals(expectedRoomId, actualRoomId);

        // Xác minh tham số isPrivate được truyền vào câu SQL là false
        verify(preparedStatement, times(1)).setBoolean(4, false);
        // Xác minh tham số passwordHash truyền vào là chuỗi rỗng
        verify(preparedStatement, times(1)).setString(5, "");
    }

    /**
     * THÔNG TIN TEST CASE 2:
     * - Phương thức test: createRoom()
     * - Thuộc logic: Tạo phòng (có mật khẩu - Private)
     * - Dữ liệu (Input):
     * + passwordHash = "hashed_abc123" (chuỗi có nội dung).
     * + ResultSet giả lập trả về ID = 205L.
     * - Kết quả mong đợi (Output):
     * + Hàm trả về ID là 205L.
     * + Biến isPrivate (tham số số 4) phải được set là TRUE.
     */
    @Test
    public void testCreateRoom_PrivateRoom_ShouldReturnGeneratedId() throws SQLException {
        // --- 1. ARRANGE ---
        long expectedRoomId = 205L;
        String passwordHash = "hashed_abc123"; // Phòng private

        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(expectedRoomId);

        // --- 2. ACT ---
        long actualRoomId = roomDao.createRoom("ROOM_VIP", "Phòng VIP", 15, 2L, passwordHash);

        // --- 3. ASSERT ---
        assertEquals(expectedRoomId, actualRoomId);

        // Xác minh tham số isPrivate được truyền vào câu SQL là true (do passwordHash hợp lệ)
        verify(preparedStatement, times(1)).setBoolean(4, true);
        verify(preparedStatement, times(1)).setString(5, passwordHash);
    }

    /**
     * THÔNG TIN TEST CASE 3:
     * - Phương thức test: createRoom()
     * - Thuộc logic: Lỗi từ phía cơ sở dữ liệu khi không sinh được ID
     * - Dữ liệu (Input):
     * + ResultSet giả lập rỗng (rs.next() trả về false).
     * - Kết quả mong đợi (Output):
     * + Phương thức ném ra SQLException với thông báo "Không tạo được room id".
     */
    @Test
    public void testCreateRoom_NoIdGenerated_ShouldThrowSQLException() throws SQLException {
        // --- 1. ARRANGE ---
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);

        // Giả lập Database không trả về khóa chính nào
        when(resultSet.next()).thenReturn(false);

        // --- 2. ACT & ASSERT ---
        // Sử dụng assertThrows để đón và kiểm tra Exception
        SQLException exception = assertThrows(SQLException.class, () -> {
            roomDao.createRoom("ERR_01", "Phòng lỗi", 15, 1L, "");
        });

        // Kiểm tra xem message lỗi ném ra có chuẩn xác không
        assertEquals("Không tạo được room id", exception.getMessage());
    }
}