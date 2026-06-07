import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class RoomServiceJoinByCodeTest {

    private RoomService roomService;
    private RoomDao roomDao;
    private AuthUser mockUser;

    @BeforeEach
    public void setUp() throws Exception {
        roomDao = mock(RoomDao.class);
        roomService = spy(new RoomService());

        // Tiêm (Inject) RoomDao mock vào RoomService
        Field daoField = RoomService.class.getDeclaredField("roomDao");
        daoField.setAccessible(true);
        daoField.set(roomService, roomDao);

        // Chuẩn bị sẵn một User hợp lệ cho mọi test case
        mockUser = new AuthUser();
        mockUser.setId(10L);
        doNothing().when(roomService).requireLogin(mockUser);
    }

    /**
     * THÔNG TIN TEST CASE 1:
     * - Phương thức test: joinByCode()
     * - Thuộc bước: 4.8 (Service validate - Nhánh: Mã phòng trống)
     * - Dữ liệu (Input):
     * + roomCode = "   " (Chỉ chứa khoảng trắng) hoặc null
     * - Kết quả mong đợi (Output):
     * + Ném ra RoomException: "Vui lòng nhập mã phòng"
     * + Không gọi xuống Database.
     */
    @Test
    public void testJoinByCode_Step4_8_BlankRoomCode_ShouldThrowException() throws Exception {
        // --- ACT & ASSERT ---
        RoomException exception = assertThrows(RoomException.class, () -> {
            roomService.joinByCode(mockUser, "   ");
        });

        assertEquals("Vui lòng nhập mã phòng", exception.getMessage());
        verify(roomDao, never()).findByCode(anyString());
    }

    /**
     * THÔNG TIN TEST CASE 2:
     * - Phương thức test: joinByCode()
     * - Thuộc bước: 4.8 (Service chuẩn hoá mã phòng & Nhánh: Không tìm thấy phòng)
     * - Dữ liệu (Input):
     * + roomCode = "  caro123  " (Cần được trim() và toUpperCase() thành "CARO123")
     * + roomDao.findByCode() trả về Optional.empty() (Không tồn tại).
     * - Kết quả mong đợi (Output):
     * + Service gọi findByCode với chuỗi đã chuẩn hoá "CARO123".
     * + Ném ra RoomException: "Phòng không tồn tại".
     */
    @Test
    public void testJoinByCode_Step4_8_RoomNotFound_ShouldThrowException() throws Exception {
        // --- 1. ARRANGE ---
        String rawRoomCode = "  caro123  ";
        String normalizedCode = "CARO123";
        when(roomDao.findByCode(normalizedCode)).thenReturn(Optional.empty());

        // --- 2. ACT & ASSERT ---
        RoomException exception = assertThrows(RoomException.class, () -> {
            roomService.joinByCode(mockUser, rawRoomCode);
        });

        assertEquals("Phòng không tồn tại", exception.getMessage());

        // Xác minh logic làm sạch dữ liệu: Đảm bảo DAO nhận được mã đã IN HOA và cắt khoảng trắng
        verify(roomDao, times(1)).findByCode(normalizedCode);
    }

    /**
     * THÔNG TIN TEST CASE 3:
     * - Phương thức test: joinByCode()
     * - Thuộc bước: 4.8 (Nhánh: Kiểm tra kết quả Join - Phòng đã đầy)
     * - Dữ liệu (Input):
     * + roomCode = "CARO99"
     * + Tìm thấy Room có id = 1L.
     * + roomDao.joinRoom() trả về enum JoinResult.FULL.
     * - Kết quả mong đợi (Output):
     * + Ném ra RoomException: "Phòng đã đầy".
     */
    @Test
    public void testJoinByCode_Step4_8_RoomFull_ShouldThrowException() throws Exception {
        // --- 1. ARRANGE ---
        Room mockRoom = new Room();
        mockRoom.setId(1L);
        when(roomDao.findByCode("CARO99")).thenReturn(Optional.of(mockRoom));

        // Giả lập kết quả phòng đã đủ 2 người
        when(roomDao.joinRoom(1L, 10L)).thenReturn(RoomDao.JoinResult.FULL);

        // --- 2. ACT & ASSERT ---
        RoomException exception = assertThrows(RoomException.class, () -> {
            roomService.joinByCode(mockUser, "CARO99");
        });

        assertEquals("Phòng đã đầy", exception.getMessage());
    }

    /**
     * THÔNG TIN TEST CASE 4:
     * - Phương thức test: joinByCode()
     * - Thuộc bước: 4.8 (Nhánh: Kiểm tra kết quả Join - Phòng đang chơi)
     * - Dữ liệu (Input):
     * + roomCode = "CARO99"
     * + Tìm thấy Room có id = 1L.
     * + roomDao.joinRoom() trả về enum JoinResult.NOT_WAITING.
     * - Kết quả mong đợi (Output):
     * + Ném ra RoomException: "Phòng không ở trạng thái chờ".
     */
    @Test
    public void testJoinByCode_Step4_8_RoomNotWaiting_ShouldThrowException() throws Exception {
        // --- 1. ARRANGE ---
        Room mockRoom = new Room();
        mockRoom.setId(1L);
        when(roomDao.findByCode("CARO99")).thenReturn(Optional.of(mockRoom));

        // Giả lập trạng thái phòng đang IN_GAME
        when(roomDao.joinRoom(1L, 10L)).thenReturn(RoomDao.JoinResult.NOT_WAITING);

        // --- 2. ACT & ASSERT ---
        RoomException exception = assertThrows(RoomException.class, () -> {
            roomService.joinByCode(mockUser, "CARO99");
        });

        assertEquals("Phòng không ở trạng thái chờ", exception.getMessage());
    }

    /**
     * THÔNG TIN TEST CASE 5:
     * - Phương thức test: joinByCode()
     * - Thuộc bước: 4.8 (Nhánh: Xử lý thành công - Trải nghiệm hoàn hảo)
     * - Dữ liệu (Input):
     * + roomCode = "vip01" (sẽ chuẩn hoá thành "VIP01")
     * + Tìm thấy Room có id = 5L.
     * + roomDao.joinRoom() trả về JOINED (hoặc một Enum đại diện cho thành công).
     * + getRoomByCode() tải lại được phòng hợp lệ.
     * - Kết quả mong đợi (Output):
     * + Trả về đối tượng Room đích.
     */
    @Test
    public void testJoinByCode_Step4_8_Success_ShouldReturnRoom() throws Exception {
        // --- 1. ARRANGE ---
        String rawRoomCode = "vip01";
        String normalizedCode = "VIP01";

        Room mockRoom = new Room();
        mockRoom.setId(5L);
        mockRoom.setRoomCode(normalizedCode);

        // Giả lập luồng hoạt động bình thường
        when(roomDao.findByCode(normalizedCode)).thenReturn(Optional.of(mockRoom));

        // Giả định JoinResult.JOINED là enum thể hiện sự thành công (tuỳ theo code thực tế của bạn)
        when(roomDao.joinRoom(5L, 10L)).thenReturn(RoomDao.JoinResult.JOINED);

        // Hàm lấy lại thông tin phòng sau khi join
        doReturn(Optional.of(mockRoom)).when(roomService).getRoomByCode(normalizedCode);

        // --- 2. ACT ---
        Room resultRoom = roomService.joinByCode(mockUser, rawRoomCode);

        // --- 3. ASSERT ---
        assertEquals(mockRoom, resultRoom);

        // Xác minh logic được gọi đầy đủ và tuần tự
        verify(roomDao, times(1)).findByCode(normalizedCode);
        verify(roomDao, times(1)).joinRoom(5L, 10L);
        verify(roomService, times(1)).getRoomByCode(normalizedCode);
    }
}