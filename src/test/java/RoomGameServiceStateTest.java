import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RoomGameServiceStateTest {

    private RoomGameService roomGameService;
    private RoomGameDao roomGameDao;

    // Các object dùng chung
    private AuthUser mockUser;
    private Room mockRoom;
    private RoomGameDao.GameInfo mockGame;

    @BeforeEach
    public void setUp() throws Exception {
        roomGameDao = mock(RoomGameDao.class);
        roomGameService = spy(new RoomGameService());

        // 1. Dùng Reflection Inject RoomGameDao vào Service
        Field field = RoomGameService.class.getDeclaredField("roomGameDao");
        field.setAccessible(true);
        field.set(roomGameService, roomGameDao);

        // 2. Chuẩn bị dữ liệu cơ sở cho mọi Test Case
        mockUser = new AuthUser();
        mockUser.setId(10L);

        mockRoom = new Room();
        mockRoom.setId(1L);
        mockRoom.setBoardSize(15);

        mockGame = mock(RoomGameDao.GameInfo.class);
        when(mockGame.id()).thenReturn(100L);

        // 3. Giả lập hàm loadRoomContext() để luôn trả về room và danh sách người chơi
        // (Giả sử RoomContext là một class/record có chứa room() và playerIds())
        Object mockContext = mock(Object.class); // Thay Object bằng class RoomContext thực tế của bạn
        doReturn(mockRoom).when(mockContext).room(); // Cần điều chỉnh theo method thực tế của record RoomContext
        doReturn(List.of(10L, 20L)).when(mockContext).playerIds();

        // Vì RoomContext là class nội bộ tự định nghĩa, ta spy trực tiếp method loadRoomContext
        doReturn(mockContext).when(roomGameService).loadRoomContext("CARO123");

        // Giả lập bước kiểm tra người chơi hợp lệ luôn thành công
        doNothing().when(roomGameService).ensureGameExists(any(), any());
    }

    /**
     * THÔNG TIN TEST CASE 1:
     * - Phương thức test: getState()
     * - Thuộc bước: 4.14 (Polling lấy trạng thái - Nhánh: Lỗi không có ván đấu)
     * - Dữ liệu (Input):
     * + findLatestGameByRoom trả về Optional.empty()
     * - Kết quả mong đợi (Output):
     * + Ném ra RoomException: "Không tìm thấy ván trong phòng".
     */
    @Test
    public void testGetState_Step4_14_NoGameFound_ShouldThrowException() throws Exception {
        // --- 1. ARRANGE ---
        when(roomGameDao.findLatestGameByRoom(mockRoom.getId())).thenReturn(Optional.empty());

        // --- 2. ACT & ASSERT ---
        RoomException exception = assertThrows(RoomException.class, () -> {
            roomGameService.getState(mockUser, "CARO123", 0);
        });

        assertEquals("Không tìm thấy ván trong phòng", exception.getMessage());
    }

    /**
     * THÔNG TIN TEST CASE 2:
     * - Phương thức test: getState()
     * - Thuộc bước: 4.14 (Nhánh: FULL SYNC - Đồng bộ toàn bộ ván cờ)
     * - Dữ liệu (Input):
     * + sinceMoveNo = -1 (Client báo chưa có dữ liệu nào)
     * - Kết quả mong đợi (Output):
     * + Lấy TOÀN BỘ nước cờ (findMoves).
     * + Gọi buildSnapshot và đánh dấu setFullSync(true).
     */
    @Test
    public void testGetState_Step4_14_NeedFullSync_ShouldReturnFullSnapshot() throws Exception {
        // --- 1. ARRANGE ---
        when(roomGameDao.findLatestGameByRoom(mockRoom.getId())).thenReturn(Optional.of(mockGame));
        when(roomGameDao.countMoves(mockGame.id())).thenReturn(5); // Ván cờ đang có 5 nước

        List<RoomGameDao.Move> mockAllMoves = List.of(mock(RoomGameDao.Move.class), mock(RoomGameDao.Move.class));
        when(roomGameDao.findMoves(mockGame.id())).thenReturn(mockAllMoves);

        RoomGameSnapshot mockSnapshot = mock(RoomGameSnapshot.class);
        doReturn(mockSnapshot).when(roomGameService).buildSnapshot(eq(mockRoom), anyList(), eq(mockGame), eq(mockAllMoves), eq(mockUser.getId()));

        // --- 2. ACT ---
        // sinceMoveNo = -1 kích hoạt điều kiện needFullSync = true
        RoomGameSnapshot result = roomGameService.getState(mockUser, "CARO123", -1);

        // --- 3. ASSERT ---
        assertEquals(mockSnapshot, result);
        verify(roomGameDao, times(1)).findMoves(mockGame.id()); // Đảm bảo query lấy tất cả nước cờ được gọi
        verify(mockSnapshot, times(1)).setFullSync(true); // Xác minh cờ FullSync đã bật cho Client
    }

    /**
     * THÔNG TIN TEST CASE 3:
     * - Phương thức test: getState()
     * - Thuộc bước: 4.14 (Nhánh: DELTA SYNC - Ván đấu đang diễn ra)
     * - Dữ liệu (Input):
     * + sinceMoveNo = 3, latestMoveNo = 4. Client chỉ thiếu 1 nước cờ.
     * + Trạng thái game: "PLAYING" (Chưa ai thắng).
     * - Kết quả mong đợi (Output):
     * + Lấy các nước cờ mới (findMovesAfter).
     * + setFullSync(false) và trả về mảng deltaMoves.
     * + KHÔNG tính toán winningCells.
     */
    @Test
    public void testGetState_Step4_14_DeltaSync_PlayingGame_ShouldReturnDeltaMoves() throws Exception {
        // --- 1. ARRANGE ---
        when(mockGame.result()).thenReturn("PLAYING");
        when(roomGameDao.findLatestGameByRoom(mockRoom.getId())).thenReturn(Optional.of(mockGame));
        when(roomGameDao.countMoves(mockGame.id())).thenReturn(4);

        // Mock 1 nước cờ mới (Nước thứ 4)
        RoomGameDao.Move mockNewMove = mock(RoomGameDao.Move.class);
        when(mockNewMove.x()).thenReturn(7);
        when(mockNewMove.y()).thenReturn(7);
        when(mockNewMove.playerNo()).thenReturn(2);
        when(mockNewMove.moveNo()).thenReturn(4);

        when(roomGameDao.findMovesAfter(mockGame.id(), 3)).thenReturn(List.of(mockNewMove));

        RoomGameSnapshot mockSnapshot = mock(RoomGameSnapshot.class);
        doReturn(mockSnapshot).when(roomGameService).baseSnapshot(eq(mockRoom), anyList(), eq(mockGame), eq(4), eq(mockUser.getId()));

        // --- 2. ACT ---
        // Client xin dữ liệu từ sau nước thứ 3
        RoomGameSnapshot result = roomGameService.getState(mockUser, "CARO123", 3);

        // --- 3. ASSERT ---
        verify(mockSnapshot, times(1)).setFullSync(false);
        // Xác minh danh sách delta move được convert đúng format int[] {x, y, playerNo, moveNo}
        verify(mockSnapshot, times(1)).setMovesDelta(argThat(list ->
                list.size() == 1 && list.get(0)[0] == 7 && list.get(0)[3] == 4
        ));

        // Xác minh KHÔNG tìm đường thắng nếu game đang chơi
        verify(roomGameService, never()).buildBoard(anyInt(), anyList());
        verify(mockSnapshot, never()).setWinningCells(any());
    }

    /**
     * THÔNG TIN TEST CASE 4:
     * - Phương thức test: getState()
     * - Thuộc bước: 4.14 (Nhánh: DELTA SYNC - Ván đấu ĐÃ KẾT THÚC)
     * - Dữ liệu (Input):
     * + Trạng thái game: "P1_WIN".
     * - Kết quả mong đợi (Output):
     * + Xử lý Delta Sync bình thường.
     * + Bắt buộc phải query ALL MOVES để gọi buildBoard.
     * + Tính toán và gắn toạ độ setWinningCells() để UI vẽ đường gạch đỏ người thắng.
     */
    @Test
    public void testGetState_Step4_14_GameFinished_ShouldCalculateWinningLine() throws Exception {
        // --- 1. ARRANGE ---
        when(mockGame.result()).thenReturn("P1_WIN"); // Người chơi 1 thắng
        when(roomGameDao.findLatestGameByRoom(mockRoom.getId())).thenReturn(Optional.of(mockGame));
        when(roomGameDao.countMoves(mockGame.id())).thenReturn(5);
        when(roomGameDao.findMovesAfter(anyLong(), anyInt())).thenReturn(List.of());

        RoomGameSnapshot mockSnapshot = mock(RoomGameSnapshot.class);
        doReturn(mockSnapshot).when(roomGameService).baseSnapshot(any(), anyList(), any(), anyInt(), anyLong());

        // Giả lập logic lấy đường thắng
        List<RoomGameDao.Move> mockAllMoves = List.of();
        when(roomGameDao.findMoves(mockGame.id())).thenReturn(mockAllMoves);

        int[][] mockBoard = new int[15][15];
        doReturn(mockBoard).when(roomGameService).buildBoard(15, mockAllMoves);

        int[][] expectedWinningCells = {{1,1}, {1,2}, {1,3}, {1,4}, {1,5}};
        doReturn(expectedWinningCells).when(roomGameService).findWinningLine(mockBoard, 1);

        // --- 2. ACT ---
        RoomGameSnapshot result = roomGameService.getState(mockUser, "CARO123", 4);

        // --- 3. ASSERT ---
        verify(mockSnapshot, times(1)).setFullSync(false);
        verify(roomGameDao, times(1)).findMoves(mockGame.id()); // Ván kết thúc nên phải tải all moves để check logic
        verify(roomGameService, times(1)).buildBoard(15, mockAllMoves);
        verify(mockSnapshot, times(1)).setWinningCells(expectedWinningCells); // Quan trọng nhất: Trả về đường thắng cho UI
    }
}