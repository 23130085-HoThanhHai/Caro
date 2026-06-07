import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class RoomServiceCreateTest {

    private RoomService roomService;
    private RoomDao roomDao;

    @BeforeEach
    public void setUp() throws Exception {
        // 1. Mock DAO
        roomDao = mock(RoomDao.class);

        // 2. Spy Service (Cho phép mock các hàm nội bộ như generateUniqueRoomCode, getRoomById)
        roomService = spy(new RoomService());

        // 3. Inject mock DAO vào service thông qua Reflection
        Field daoField = RoomService.class.getDeclaredField("roomDao");
        daoField.setAccessible(true);
        daoField.set(roomService, roomDao);
    }

    /**
     * THÔNG TIN TEST CASE 1:
     * - Phương thức test: createRoom()
     * - Thuộc logic: 4.7 Kiểm tra tính hợp lệ của tên phòng (Validation)
     * - Dữ liệu (Input):
     * + Tên phòng là chuỗi rỗng ("   ") hoặc null.
     * - Kết quả mong đợi (Output):
     * + Hàm ném ra RoomException với thông báo "Tên phòng không được để trống".
     * + Database không hề bị tác động (createRoom của DAO không được gọi).
     */
    @Test
    public void testCreateRoom_BlankRoomName_ShouldThrowException() throws Exception {
        // --- 1. ARRANGE ---
        AuthUser authUser = new AuthUser();
        authUser.setId(100L);

        // Giả lập hàm requireLogin chạy thành công không báo lỗi
        doNothing().when(roomService).requireLogin(authUser);

        // --- 2. ACT & ASSERT ---
        RoomException exception = assertThrows(RoomException.class, () -> {
            roomService.createRoom(authUser, "   ", 15);
        });

        assertEquals("Tên phòng không được để trống", exception.getMessage());
        // Xác minh không có truy vấn nào được gửi xuống Database
        verify(roomDao, never()).createRoom(anyString(), anyString(), anyInt(), anyLong(), any());
    }

    /**
     * THÔNG TIN TEST CASE 2:
     * - Phương thức test: createRoom()
     * - Thuộc logic: Kiểm tra tính hợp lệ của kích thước bàn cờ
     * - Dữ liệu (Input):
     * + Kích thước bàn cờ (boardSize) = 3 (Nhỏ hơn mức tối thiểu là 5).
     * - Kết quả mong đợi (Output):
     * + Ném ra RoomException báo lỗi kích thước không hợp lệ.
     */
    @Test
    public void testCreateRoom_InvalidBoardSize_ShouldThrowException() throws Exception {
        // --- 1. ARRANGE ---
        AuthUser authUser = new AuthUser();
        doNothing().when(roomService).requireLogin(authUser);

        // --- 2. ACT & ASSERT ---
        RoomException exception = assertThrows(RoomException.class, () -> {
            // boardSize = 3 (sai quy định 5-50)
            roomService.createRoom(authUser, "Phòng Cờ Caro Giao Lưu", 3);
        });

        assertEquals("Kích thước bàn cờ phải từ 5 đến 50", exception.getMessage());
        verify(roomDao, never()).createRoom(anyString(), anyString(), anyInt(), anyLong(), any());
    }

    /**
     * THÔNG TIN TEST CASE 3:
     * - Phương thức test: createRoom()
     * - Thuộc logic: Luồng tạo phòng thành công (Happy Path)
     * - Dữ liệu (Input):
     * + Dữ liệu hợp lệ: Tên phòng "Caro Siêu Cấp", Bàn cờ 15x15.
     * + Mock hệ thống sinh mã phòng trả về "CARO99".
     * + Mock DAO lưu thành công và trả về ID = 1L.
     * - Kết quả mong đợi (Output):
     * + Hàm gọi đúng các thứ tự: createRoom (DAO) -> addHostToRoom (DAO).
     * + Trả về đối tượng Room tương ứng.
     */
    @Test
    public void testCreateRoom_ValidData_ShouldCreateAndReturnRoom() throws Exception {
        // --- 1. ARRANGE ---
        AuthUser authUser = new AuthUser();
        authUser.setId(100L);
        long newRoomId = 1L;
        String generatedCode = "CARO99";

        Room expectedRoom = new Room();
        expectedRoom.setId(newRoomId);
        expectedRoom.setRoomCode(generatedCode);

        // Giả lập các bước nội bộ không bị lỗi
        doNothing().when(roomService).requireLogin(authUser);
        doReturn(generatedCode).when(roomService).generateUniqueRoomCode();

        // Giả lập DAO thao tác DB thành công
        when(roomDao.createRoom(generatedCode, "Caro Siêu Cấp", 15, 100L, null)).thenReturn(newRoomId);
        doNothing().when(roomDao).addHostToRoom(newRoomId, 100L);

        // Giả lập hàm getRoomById lấy lại được phòng vừa tạo
        doReturn(Optional.of(expectedRoom)).when(roomService).getRoomById(newRoomId);

        // --- 2. ACT ---
        Room actualRoom = roomService.createRoom(authUser, "Caro Siêu Cấp", 15);

        // --- 3. ASSERT ---
        assertEquals(expectedRoom, actualRoom);

        // Xác minh DAO được gọi đúng với các tham số đã làm sạch (như trim() tên phòng)
        verify(roomDao, times(1)).createRoom(generatedCode, "Caro Siêu Cấp", 15, 100L, null);
        verify(roomDao, times(1)).addHostToRoom(newRoomId, 100L);
    }

    /**
     * THÔNG TIN TEST CASE 4:
     * - Phương thức test: createRoom()
     * - Thuộc logic: Xử lý rủi ro mất đồng bộ dữ liệu sau khi INSERT
     * - Dữ liệu (Input):
     * + Dữ liệu hợp lệ, đã gọi tạo phòng trong DB thành công.
     * + Tuy nhiên, hàm getRoomById() lại không tìm thấy dữ liệu (trả về Optional.empty()).
     * - Kết quả mong đợi (Output):
     * + Ném ra lỗi RoomException do hàm .orElseThrow() kích hoạt.
     */
    @Test
    public void testCreateRoom_LoadRoomFails_ShouldThrowException() throws Exception {
        // --- 1. ARRANGE ---
        AuthUser authUser = new AuthUser();
        authUser.setId(100L);
        long newRoomId = 1L;

        doNothing().when(roomService).requireLogin(authUser);
        doReturn("XYZ123").when(roomService).generateUniqueRoomCode();
        when(roomDao.createRoom(anyString(), anyString(), anyInt(), anyLong(), any())).thenReturn(newRoomId);

        // Giả lập rủi ro: Tìm lại phòng vừa tạo nhưng không thấy (DB lỗi delay hoặc transaction chưa commit)
        doReturn(Optional.empty()).when(roomService).getRoomById(newRoomId);

        // --- 2. ACT & ASSERT ---
        RoomException exception = assertThrows(RoomException.class, () -> {
            roomService.createRoom(authUser, "Phòng Test", 20);
        });

        assertEquals("Không tải được phòng vừa tạo", exception.getMessage());
    }
}