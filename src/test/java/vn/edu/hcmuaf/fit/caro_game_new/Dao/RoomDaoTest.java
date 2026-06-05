package vn.edu.hcmuaf.fit.caro_game_new.room;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.hcmuaf.fit.caro_game_new.DB.DbUtil;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomDaoTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    private RoomDao roomDao;
    private MockedStatic<DbUtil> mockedDbUtil;

    @BeforeEach
    void setUp() {
        roomDao = new RoomDao();
        // Tạo một mock tĩnh cho DbUtil nằm trong package mới để chặn các kết nối DB thực tế
        mockedDbUtil = Mockito.mockStatic(DbUtil.class);
    }

    @AfterEach
    void tearDown() {
        // Giải phóng luồng mock tĩnh sau khi hoàn thành ca test
        mockedDbUtil.close();
    }

    @Test
    void testCreateRoom_Success_ReturnsGeneratedId() throws SQLException {
        // Given
        String roomCode = "CR666X";
        String roomName = "Đấu Trường Caro";
        int boardSize = 15;
        long hostId = 10L;
        long expectedRoomId = 999L;

        mockedDbUtil.when(DbUtil::getConnection).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong(1)).thenReturn(expectedRoomId);

        // When
        long actualRoomId = roomDao.createRoom(roomCode, roomName, boardSize, hostId, null);

        // Then
        assertEquals(expectedRoomId, actualRoomId);
        verify(mockPreparedStatement).setString(1, roomCode);
        verify(mockPreparedStatement).setString(2, roomName);
        verify(mockPreparedStatement).setLong(3, hostId);
        verify(mockPreparedStatement).setBoolean(4, false); // passwordHash nhận vào null -> false
        verify(mockPreparedStatement).setInt(6, boardSize);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testAddHostToRoom_Success() throws SQLException {
        // Given
        long roomId = 999L;
        long hostId = 10L;

        mockedDbUtil.when(DbUtil::getConnection).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // When & Then
        assertDoesNotThrow(() -> roomDao.addHostToRoom(roomId, hostId));

        verify(mockPreparedStatement).setLong(1, roomId);
        verify(mockPreparedStatement).setLong(2, hostId);
        verify(mockPreparedStatement).executeUpdate();
    }
}