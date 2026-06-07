import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RoomStatusTest {

    // ==========================================
    // 1. TEST CHO PHƯƠNG THỨC fromDbValue()
    // ==========================================

    /**
     * THÔNG TIN TEST CASE 1:
     * - Phương thức test: fromDbValue(String)
     * - Thuộc bước: 4.9 (Ánh xạ dữ liệu khi đọc từ Database lên)
     * - Dữ liệu (Input): giá trị null
     * - Kết quả mong đợi (Output):
     * + Trả về RoomStatus.WAITING (Xử lý an toàn tránh NullPointerException).
     */
    @Test
    public void testFromDbValue_Step4_9_NullInput_ShouldReturnWaiting() {
        // --- ACT ---
        RoomStatus status = RoomStatus.fromDbValue(null);

        // --- ASSERT ---
        assertEquals(RoomStatus.WAITING, status);
    }

    /**
     * THÔNG TIN TEST CASE 2:
     * - Phương thức test: fromDbValue(String)
     * - Thuộc bước: 4.9 (Nhánh chuyển đổi sang trạng thái WAITING)
     * - Dữ liệu (Input):
     * + Các chuỗi hợp lệ: "OPEN", "WAITING" (bao gồm cả viết thường "open" để test toUpperCase).
     * - Kết quả mong đợi (Output):
     * + Trả về RoomStatus.WAITING.
     */
    @Test
    public void testFromDbValue_Step4_9_OpenOrWaitingString_ShouldReturnWaiting() {
        assertEquals(RoomStatus.WAITING, RoomStatus.fromDbValue("OPEN"));
        assertEquals(RoomStatus.WAITING, RoomStatus.fromDbValue("WAITING"));

        // Test thêm trường hợp chữ thường xem hàm toUpperCase() có hoạt động đúng không
        assertEquals(RoomStatus.WAITING, RoomStatus.fromDbValue("open"));
    }

    /**
     * THÔNG TIN TEST CASE 3:
     * - Phương thức test: fromDbValue(String)
     * - Thuộc bước: 4.9 (Nhánh chuyển đổi sang trạng thái PLAYING)
     * - Dữ liệu (Input):
     * + Các chuỗi hợp lệ: "IN_GAME", "PLAYING" (kể cả viết thường).
     * - Kết quả mong đợi (Output):
     * + Trả về RoomStatus.PLAYING.
     */
    @Test
    public void testFromDbValue_Step4_9_InGameOrPlayingString_ShouldReturnPlaying() {
        assertEquals(RoomStatus.PLAYING, RoomStatus.fromDbValue("IN_GAME"));
        assertEquals(RoomStatus.PLAYING, RoomStatus.fromDbValue("PLAYING"));
        assertEquals(RoomStatus.PLAYING, RoomStatus.fromDbValue("in_game"));
    }

    /**
     * THÔNG TIN TEST CASE 4:
     * - Phương thức test: fromDbValue(String)
     * - Thuộc bước: 4.9 (Nhánh Default - Các trạng thái không xác định hoặc đóng)
     * - Dữ liệu (Input):
     * + Các chuỗi rác như "XYZ", hoặc chuỗi "CLOSED".
     * - Kết quả mong đợi (Output):
     * + Trả về RoomStatus.CLOSED để đảm bảo an toàn.
     */
    @Test
    public void testFromDbValue_Step4_9_UnknownOrClosedString_ShouldReturnClosed() {
        assertEquals(RoomStatus.CLOSED, RoomStatus.fromDbValue("CLOSED"));
        assertEquals(RoomStatus.CLOSED, RoomStatus.fromDbValue("XYZ_UNDEFINED"));
        assertEquals(RoomStatus.CLOSED, RoomStatus.fromDbValue("")); // Chuỗi rỗng
    }

    // ==========================================
    // 2. TEST CHO PHƯƠNG THỨC toDbValue()
    // ==========================================

    /**
     * THÔNG TIN TEST CASE 5:
     * - Phương thức test: toDbValue()
     * - Thuộc bước: 4.9 (Chuẩn bị dữ liệu để INSERT/UPDATE xuống Database)
     * - Dữ liệu (Input):
     * + Lần lượt gọi toDbValue() trên 3 Enum: WAITING, PLAYING, CLOSED.
     * - Kết quả mong đợi (Output):
     * + Trả về chuỗi String chính xác khớp với schema DB ("OPEN", "IN_GAME", "CLOSED").
     */
    @Test
    public void testToDbValue_Step4_9_AllEnums_ShouldReturnCorrectString() {
        // --- ASSERT ---
        assertEquals("OPEN", RoomStatus.WAITING.toDbValue());
        assertEquals("IN_GAME", RoomStatus.PLAYING.toDbValue());
        assertEquals("CLOSED", RoomStatus.CLOSED.toDbValue());
    }
}