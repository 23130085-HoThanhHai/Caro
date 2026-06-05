import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.sql.SQLException;

import static org.mockito.Mockito.*;

public class JoinRoomServletPostTest {

    private JoinRoomServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;
    private RoomService roomService;

    @BeforeEach
    public void setUp() throws Exception {
        // 1. Khởi tạo các Mock objects
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);
        roomService = mock(RoomService.class);

        servlet = spy(new JoinRoomServlet());

        // 2. Kỹ thuật Reflection: Bơm (inject) mock roomService vào servlet
        // Cần thiết vì khai báo gốc của bạn là: private final RoomService roomService = new RoomService();
        Field field = JoinRoomServlet.class.getDeclaredField("roomService");
        field.setAccessible(true);
        field.set(servlet, roomService);

        // 3. Chặn lỗi NullPointer khi hệ thống gọi hàm log() của GenericServlet trong khối catch
        doNothing().when(servlet).log(anyString(), any(Throwable.class));
    }

    /**
     * THÔNG TIN TEST CASE 1:
     * - Phương thức test: doPost()
     * - Thuộc bước: 4.6 (Trình duyệt gửi HTTP POST /join-room - Nhánh: THÀNH CÔNG)
     * - Dữ liệu giả lập (Input):
     * + roomCode = "CARO123"
     * + Dữ liệu session chứa AuthUser hợp lệ.
     * + roomService.joinByCode() trả về một đối tượng Room hợp lệ.
     * - Kết quả mong đợi (Output):
     * + Set character encoding UTF-8 thành công.
     * + Servlet chuyển hướng (redirect) người chơi tới trang bàn cờ "/caro-game/room?code=CARO123".
     */
    @Test
    public void testDoPost_Step4_6_JoinSuccess_ShouldRedirectToRoom() throws Exception {
        // --- 1. ARRANGE ---
        AuthUser mockUser = new AuthUser();
        Room mockRoom = new Room();
        mockRoom.setRoomCode("CARO123");

        // Giả lập lấy Session và User
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthSession.AUTH_USER)).thenReturn(mockUser);

        // Giả lập lấy Parameter
        when(request.getParameter("roomCode")).thenReturn("CARO123");
        when(request.getContextPath()).thenReturn("/caro-game");

        // Giả lập RoomService cho phép join thành công
        when(roomService.joinByCode(mockUser, "CARO123")).thenReturn(mockRoom);

        // --- 2. ACT ---
        servlet.doPost(request, response);

        // --- 3. ASSERT ---
        verify(request, times(1)).setCharacterEncoding("UTF-8");
        verify(response, times(1)).sendRedirect("/caro-game/room?code=CARO123");
        verify(request, never()).getRequestDispatcher(anyString());
    }

    /**
     * THÔNG TIN TEST CASE 2:
     * - Phương thức test: doPost()
     * - Thuộc bước: 4.6 (Nhánh Lỗi Nghiệp Vụ: Sai mã phòng, phòng đã đầy, phòng đang chơi)
     * - Dữ liệu giả lập (Input):
     * + roomCode = "SAI_MA"
     * + roomService.joinByCode() ném ra ngoại lệ RoomException với câu thông báo lỗi.
     * - Kết quả mong đợi (Output):
     * + Bắt được catch (RoomException e).
     * + Set attribute "error" bằng chính câu thông báo lỗi của Exception.
     * + Giữ lại "roomCode" để hiển thị lại trên form.
     * + Forward trở lại trang "join-room.jsp".
     */
    @Test
    public void testDoPost_Step4_6_RoomException_ShouldForwardWithErrorMsg() throws Exception {
        // --- 1. ARRANGE ---
        AuthUser mockUser = new AuthUser();

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthSession.AUTH_USER)).thenReturn(mockUser);
        when(request.getParameter("roomCode")).thenReturn("SAI_MA");

        // Giả lập người chơi nhập sai mã hoặc phòng đã đầy
        when(roomService.joinByCode(mockUser, "SAI_MA"))
                .thenThrow(new RoomException("Mã phòng không tồn tại hoặc phòng đã đầy"));

        when(request.getRequestDispatcher("/WEB-INF/jsp/room/join-room.jsp")).thenReturn(dispatcher);

        // --- 2. ACT ---
        servlet.doPost(request, response);

        // --- 3. ASSERT ---
        verify(request, times(1)).setAttribute("error", "Mã phòng không tồn tại hoặc phòng đã đầy");
        verify(request, times(1)).setAttribute("roomCode", "SAI_MA");
        verify(request, times(1)).getRequestDispatcher("/WEB-INF/jsp/room/join-room.jsp");
        verify(dispatcher, times(1)).forward(request, response);

        // Tuyệt đối không được redirect
        verify(response, never()).sendRedirect(anyString());
    }

    /**
     * THÔNG TIN TEST CASE 3:
     * - Phương thức test: doPost()
     * - Thuộc bước: 4.6 (Nhánh Lỗi Hệ Thống: Mất kết nối Database, sập server CSDL)
     * - Dữ liệu giả lập (Input):
     * + roomService.joinByCode() ném ra ngoại lệ SQLException.
     * - Kết quả mong đợi (Output):
     * + Bắt được catch (SQLException e).
     * + Hàm log() được gọi để ghi nhận lỗi "Join room failed".
     * + Thông báo lỗi che giấu chi tiết DB, hiện câu thông báo chung: "Không thể vào phòng lúc này...".
     * + Forward trở lại trang "join-room.jsp".
     */
    @Test
    public void testDoPost_Step4_6_SQLException_ShouldLogAndForwardWithGenericError() throws Exception {
        // --- 1. ARRANGE ---
        AuthUser mockUser = new AuthUser();

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthSession.AUTH_USER)).thenReturn(mockUser);
        when(request.getParameter("roomCode")).thenReturn("CARO_ERROR");

        // Giả lập Database bị sập khi đang query
        when(roomService.joinByCode(mockUser, "CARO_ERROR")).thenThrow(new SQLException("DB Connection lost"));
        when(request.getRequestDispatcher("/WEB-INF/jsp/room/join-room.jsp")).thenReturn(dispatcher);

        // --- 2. ACT ---
        servlet.doPost(request, response);

        // --- 3. ASSERT ---
        // Xác minh hệ thống có ghi log lỗi ra console (Rất quan trọng cho DevOps/Maintain sau này)
        verify(servlet, times(1)).log(eq("Join room failed"), any(SQLException.class));

        // Xác minh UI nhận được thông báo lỗi chung chung (an toàn bảo mật)
        verify(request, times(1)).setAttribute("error", "Không thể vào phòng lúc này, vui lòng thử lại");
        verify(request, times(1)).setAttribute("roomCode", "CARO_ERROR");
        verify(request, times(1)).getRequestDispatcher("/WEB-INF/jsp/room/join-room.jsp");
        verify(dispatcher, times(1)).forward(request, response);
    }
}