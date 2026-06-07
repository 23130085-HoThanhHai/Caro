package vn.edu.hcmuaf.fit.caro_game_new.room;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.hcmuaf.fit.caro_game_new.model.AuthUser;
import vn.edu.hcmuaf.fit.caro_game_new.model.Room;
import vn.edu.hcmuaf.fit.caro_game_new.service.RoomException;
import vn.edu.hcmuaf.fit.caro_game_new.service.RoomService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateRoomServletTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private RequestDispatcher requestDispatcher;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private CreateRoomServlet servlet;

    @BeforeEach
    void setUp() throws Exception {
        // Sử dụng Reflection để thay thế instance RoomService được khởi tạo cứng bên trong Servlet bằng mock tương ứng
        Field serviceField = CreateRoomServlet.class.getDeclaredField("roomService");
        serviceField.setAccessible(true);
        serviceField.set(servlet, roomService);
    }

    @Test
    void testDoGet_WhenNotLoggedIn_RedirectsToLogin() throws ServletException, IOException {
        // Given
        when(request.getSession(false)).thenReturn(null);
        when(request.getContextPath()).thenReturn("/caro");

        // When
        servlet.doGet(request, response);

        // Then
        verify(response).sendRedirect("/caro/login");
    }

    @Test
    void testDoGet_WhenLoggedIn_ForwardsToCreateRoomJsp() throws ServletException, IOException {
        // Given
        AuthUser mockUser = new AuthUser();
        when(request.getSession(false)).thenReturn(session);
        // Lưu ý: Đã sửa lại việc lấy thuộc tính động thông qua session nếu bạn cấu hình lớp AuthSession tương ứng trong package mới
        when(session.getAttribute(anyString())).thenReturn(mockUser);
        when(request.getRequestDispatcher("/WEB-INF/jsp/room/create-room.jsp")).thenReturn(requestDispatcher);

        // When
        servlet.doGet(request, response);

        // Then
        verify(requestDispatcher).forward(request, response);
    }

    @Test
    void testDoPost_Success_RedirectsToRoomDetails() throws ServletException, IOException, SQLException, RoomException {
        // Given
        AuthUser mockUser = new AuthUser();
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(anyString())).thenReturn(mockUser);

        when(request.getParameter("roomName")).thenReturn("Phòng Đại Chiến");
        when(request.getParameter("boardSize")).thenReturn("15");
        when(request.getContextPath()).thenReturn("/caro");

        Room mockCreatedRoom = new Room();
        mockCreatedRoom.setRoomCode("XYZ123");
        when(roomService.createRoom(eq(mockUser), eq("Phòng Đại Chiến"), eq(15))).thenReturn(mockCreatedRoom);

        // When
        servlet.doPost(request, response);

        // Then
        verify(response).sendRedirect("/caro/room?code=XYZ123");
    }

    @Test
    void testDoPost_ValidationFailure_ForwardsBackWithError() throws ServletException, IOException, SQLException, RoomException {
        // Given
        AuthUser mockUser = new AuthUser();
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(anyString())).thenReturn(mockUser);

        when(request.getParameter("roomName")).thenReturn("");
        when(request.getParameter("boardSize")).thenReturn("15");
        when(request.getRequestDispatcher("/WEB-INF/jsp/room/create-room.jsp")).thenReturn(requestDispatcher);

        when(roomService.createRoom(eq(mockUser), eq(""), eq(15)))
                .thenThrow(new RoomException("Tên phòng không được để trống"));

        // When
        servlet.doPost(request, response);

        // Then
        verify(request).setAttribute("error", "Tên phòng không được để trống");
        verify(request).setAttribute("roomName", "");
        verify(request).setAttribute("boardSize", 15);
        verify(requestDispatcher).forward(request, response);
    }
}