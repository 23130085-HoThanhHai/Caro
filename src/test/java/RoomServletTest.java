import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class RoomServletTest {

    private RoomServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;
    private RoomService roomService;

    @BeforeEach
    public void setUp() throws Exception {
        // 1. Khởi tạo mock objects
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);
        roomService = mock(RoomService.class);

        servlet = spy(new RoomServlet());

        // 2. Dùng Reflection để inject mock RoomService vào Servlet
        Field field = RoomServlet.class.getDeclaredField("roomService");
        field.setAccessible(true);
        field.set(servlet, roomService);

        // 3. Chặn hàm log() của GenericServlet để không bị lỗi NullPointer khi test nhánh Exception
        doNothing().when(servlet).log(anyString(), any(Throwable.class));

        // Giả lập ContextPath chung cho các bài test có dùng redirect
        when(request.getContextPath()).thenReturn("/caro-game");
    }

    /**
     * THÔNG TIN TEST CASE 1:
     * - Phương thức test: doGet()
     * - Thuộc logic: 4.10 Server gửi lệnh chuyển hướng (Redirect), Kiểm tra bảo mật (Authentication)
     * - Dữ liệu (Input):
     * + Session là null (Chưa đăng nhập) HOẶC không có AuthUser trong session.
     * - Kết quả mong đợi (Output):
     * + Redirect về trang "/caro-game/login".
     * + Ngừng thực thi ngay lập tức (return).
     */
    @Test
    public void testDoGet_UserNotLoggedIn_ShouldRedirectToLogin() throws Exception {
        // --- 1. ARRANGE ---
        when(request.getSession(false)).thenReturn(null);

        // --- 2. ACT ---
        servlet.doGet(request, response);

        // --- 3. ASSERT ---
        verify(response, times(1)).sendRedirect("/caro-game/login");
        verify(request, never()).getParameter(anyString()); // Đảm bảo không chạy xuống dòng dưới
    }

    /**
     * THÔNG TIN TEST CASE 2:
     * - Phương thức test: doGet()
     * - Thuộc logic: Kiểm tra tham số (Validation)
     * - Dữ liệu (Input):
     * + User đã đăng nhập hợp lệ.
     * + Tham số "code" là null hoặc chuỗi rỗng ("   ").
     * - Kết quả mong đợi (Output):
     * + Redirect về trang tìm phòng "/caro-game/find-room".
     */
    @Test
    public void testDoGet_BlankRoomCode_ShouldRedirectToFindRoom() throws Exception {
        // --- 1. ARRANGE ---
        AuthUser mockUser = new AuthUser();
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthSession.AUTH_USER)).thenReturn(mockUser);

        // Cố tình truyền mã phòng rỗng
        when(request.getParameter("code")).thenReturn("   ");

        // --- 2. ACT ---
        servlet.doGet(request, response);

        // --- 3. ASSERT ---
        verify(response, times(1)).sendRedirect("/caro-game/find-room");
        verify(roomService, never()).getRoomByCode(anyString()); // Không được gọi DB
    }

    /**
     * THÔNG TIN TEST CASE 3:
     * - Phương thức test: doGet()
     * - Thuộc logic: Tải dữ liệu phòng - Nhánh phòng không tồn tại
     * - Dữ liệu (Input):
     * + "code" = "CARO99"
     * + roomService.getRoomByCode() trả về Optional.empty().
     * - Kết quả mong đợi (Output):
     * + Set Attribute "error" = "Không tìm thấy phòng".
     * + Forward người dùng quay lại trang nhập mã "/WEB-INF/jsp/room/join-room.jsp".
     */
    @Test
    public void testDoGet_RoomNotFound_ShouldForwardToJoinRoomWithError() throws Exception {
        // --- 1. ARRANGE ---
        AuthUser mockUser = new AuthUser();
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthSession.AUTH_USER)).thenReturn(mockUser);
        when(request.getParameter("code")).thenReturn("CARO99");

        when(roomService.getRoomByCode("CARO99")).thenReturn(Optional.empty());
        when(request.getRequestDispatcher("/WEB-INF/jsp/room/join-room.jsp")).thenReturn(dispatcher);

        // --- 2. ACT ---
        servlet.doGet(request, response);

        // --- 3. ASSERT ---
        verify(request, times(1)).setAttribute("error", "Không tìm thấy phòng");
        verify(request, times(1)).getRequestDispatcher("/WEB-INF/jsp/room/join-room.jsp");
        verify(dispatcher, times(1)).forward(request, response);
    }

    /**
     * THÔNG TIN TEST CASE 4:
     * - Phương thức test: doGet()
     * - Thuộc logic: Xử lý ngoại lệ hệ thống (SQLException)
     * - Dữ liệu (Input):
     * + roomService ném ra lỗi SQLException khi đang truy vấn DB.
     * - Kết quả mong đợi (Output):
     * + Ghi log lỗi "Load room failed".
     * + Set Attribute "error" = "Không thể tải phòng".
     * + Forward về giao diện join-room.jsp.
     */
    @Test
    public void testDoGet_SQLException_ShouldLogAndForwardWithError() throws Exception {
        // --- 1. ARRANGE ---
        AuthUser mockUser = new AuthUser();
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthSession.AUTH_USER)).thenReturn(mockUser);
        when(request.getParameter("code")).thenReturn("ERROR_CODE");

        when(roomService.getRoomByCode("ERROR_CODE")).thenThrow(new SQLException("Lỗi sập DB"));
        when(request.getRequestDispatcher("/WEB-INF/jsp/room/join-room.jsp")).thenReturn(dispatcher);

        // --- 2. ACT ---
        servlet.doGet(request, response);

        // --- 3. ASSERT ---
        verify(servlet, times(1)).log(eq("Load room failed"), any(SQLException.class));
        verify(request, times(1)).setAttribute("error", "Không thể tải phòng");
        verify(dispatcher, times(1)).forward(request, response);
    }

    /**
     * THÔNG TIN TEST CASE 5:
     * - Phương thức test: doGet()
     * - Thuộc logic: Tải bàn cờ thành công (Happy Path)
     * - Dữ liệu (Input):
     * + Tham số "code" = " vip1 " (Có khoảng trắng, chữ thường để test trim() và toUpperCase()).
     * + AuthUser có ID = 100L.
     * + Tìm thấy Room từ DB.
     * - Kết quả mong đợi (Output):
     * + Hàm gọi tới Service với mã đã chuẩn hoá "VIP1".
     * + Set Attribute "room" = đối tượng Room.
     * + Set Attribute "currentUserId" = 100L.
     * + Forward sang trang giao diện bàn cờ "/WEB-INF/jsp/room/room.jsp".
     */
    @Test
    public void testDoGet_Success_ShouldForwardToRoomJsp() throws Exception {
        // --- 1. ARRANGE ---
        AuthUser mockUser = new AuthUser();
        mockUser.setId(100L); // Cài đặt ID cho user

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthSession.AUTH_USER)).thenReturn(mockUser);

        // Truyền tham số chưa chuẩn hoá
        when(request.getParameter("code")).thenReturn(" vip1 ");

        Room mockRoom = new Room();
        when(roomService.getRoomByCode("VIP1")).thenReturn(Optional.of(mockRoom));

        // Điểm đến của thành công là trang room.jsp
        when(request.getRequestDispatcher("/WEB-INF/jsp/room/room.jsp")).thenReturn(dispatcher);

        // --- 2. ACT ---
        servlet.doGet(request, response);

        // --- 3. ASSERT ---
        // Xác minh dữ liệu được nhét vào request đúng đắn để JSP có thể hiển thị
        verify(request, times(1)).setAttribute("room", mockRoom);
        verify(request, times(1)).setAttribute("currentUserId", 100L);

        // Kiểm tra đích đến
        verify(request, times(1)).getRequestDispatcher("/WEB-INF/jsp/room/room.jsp");
        verify(dispatcher, times(1)).forward(request, response);
    }
}