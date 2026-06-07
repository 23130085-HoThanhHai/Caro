package vn.edu.hcmuaf.fit.caro_game_new.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.hcmuaf.fit.caro_game_new.Dao.RoomDao;
import vn.edu.hcmuaf.fit.caro_game_new.model.AuthUser;
import vn.edu.hcmuaf.fit.caro_game_new.model.Room;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceTest {

    @Mock
    private RoomDao roomDao;

    @InjectMocks
    private RoomService roomService;

    private AuthUser mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new AuthUser();
        mockUser.setId(1L);
        mockUser.setUsername("caroplayer");
    }

    @Test
    void testCreateRoom_Success() throws SQLException, RoomException {
        // Given
        String roomName = "Phòng Thách Đấu";
        int boardSize = 19;
        long generatedId = 88L;

        when(roomDao.existsByCode(anyString())).thenReturn(false);
        when(roomDao.createRoom(anyString(), eq(roomName), eq(boardSize), eq(mockUser.getId()), any())).thenReturn(generatedId);
        doNothing().when(roomDao).addHostToRoom(generatedId, mockUser.getId());

        Room expectedRoom = new Room();
        expectedRoom.setId(generatedId);
        expectedRoom.setRoomCode("CARO99");
        when(roomDao.findById(generatedId)).thenReturn(Optional.of(expectedRoom));

        // When
        Room actualRoom = roomService.createRoom(mockUser, roomName, boardSize);

        // Then
        assertNotNull(actualRoom);
        assertEquals(generatedId, actualRoom.getId());
        verify(roomDao, times(1)).createRoom(anyString(), eq(roomName), eq(boardSize), eq(mockUser.getId()), any());
        verify(roomDao, times(1)).addHostToRoom(generatedId, mockUser.getId());
    }

    @Test
    void testCreateRoom_ThrowException_WhenUserNotLoggedIn() {
        // When & Then
        RoomException exception = assertThrows(RoomException.class, () -> {
            roomService.createRoom(null, "Phòng Solo", 15);
        });
        assertEquals("Bạn cần đăng nhập để dùng chức năng phòng", exception.getMessage());
    }

    @Test
    void testCreateRoom_ThrowException_WhenRoomNameIsEmpty() {
        // When & Then
        RoomException exception = assertThrows(RoomException.class, () -> {
            roomService.createRoom(mockUser, "   ", 15);
        });
        assertEquals("Tên phòng không được để trống", exception.getMessage());
    }

    @Test
    void testCreateRoom_ThrowException_WhenRoomNameTooLong() {
        // Given: Tên phòng vượt quá giới hạn 100 ký tự
        String longRoomName = "k".repeat(101);

        // When & Then
        RoomException exception = assertThrows(RoomException.class, () -> {
            roomService.createRoom(mockUser, longRoomName, 15);
        });
        assertEquals("Tên phòng tối đa 100 ký tự", exception.getMessage());
    }

    @Test
    void testCreateRoom_ThrowException_WhenBoardSizeInvalid() {
        // When & Then (Nhỏ hơn kích thước tối thiểu là 5)
        RoomException exceptionMin = assertThrows(RoomException.class, () -> {
            roomService.createRoom(mockUser, "Caro Club", 4);
        });
        assertEquals("Kích thước bàn cờ phải từ 5 đến 50", exceptionMin.getMessage());

        // When & Then (Lớn hơn kích thước tối đa là 50)
        RoomException exceptionMax = assertThrows(RoomException.class, () -> {
            roomService.createRoom(mockUser, "Caro Club", 51);
        });
        assertEquals("Kích thước bàn cờ phải từ 5 đến 50", exceptionMax.getMessage());
    }
}