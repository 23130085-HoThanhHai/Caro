import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

public class JoinRoomServletTest {

    private JoinRoomServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private RequestDispatcher dispatcher;

    @BeforeEach
    public void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        dispatcher = mock(RequestDispatcher.class);
        servlet = spy(new JoinRoomServlet());
    }

    /**
     * THÔNG TIN TEST CASE 1:
     * - Phương thức test: doGet(HttpServletRequest, HttpServletResponse)
     * - Thuộc bước: 4.3 (Server kiểm tra đăng nhập - Nhánh: Bị từ chối)
     * - Dữ liệu giả lập (Input):
     * + Trạng thái người dùng: CHƯA đăng nhập (isLoggedIn = false)
     * + Context path của ứng dụng: "/caro-game"
     * - Kết quả test mong đợi (Output):
     * + Hệ thống bắt buộc phải chuyển hướng (redirect) người chơi về trang "/caro-game/login".
     * + Không được phép hiển thị trang join-room.
     */
    @Test
    public void testDoGet_Step4_3_UserNotLoggedIn_ShouldRedirectToLogin() throws Exception {
        // --- 1. ARRANGE (Chuẩn bị dữ liệu) ---
        doReturn(false).when(servlet).isLoggedIn(request);
        when(request.getContextPath()).thenReturn("/caro-game");

        // --- 2. ACT (Thực thi bước 4.2: Trình duyệt gọi GET /find-room) ---
        servlet.doGet(request, response);

        // --- 3. ASSERT (Kiểm tra kết quả bước 4.3) ---
        // Xác nhận hàm sendRedirect được gọi đúng 1 lần với URL dẫn tới trang đăng nhập
        verify(response, times(1)).sendRedirect("/caro-game/login");

        // Đảm bảo không có lệnh forward nào được thực thi để tránh rò rỉ giao diện
        verify(request, never()).getRequestDispatcher(anyString());
    }

    /**
     * THÔNG TIN TEST CASE 2:
     * - Phương thức test: doGet(HttpServletRequest, HttpServletResponse)
     * - Thuộc bước: 4.3 (Server kiểm tra đăng nhập và forward trang nhập mã phòng - Nhánh: Thành công)
     * - Dữ liệu giả lập (Input):
     * + Trạng thái người dùng: ĐÃ đăng nhập hợp lệ (isLoggedIn = true)
     * + File giao diện đích: "/WEB-INF/jsp/room/join-room.jsp"
     * - Kết quả test mong đợi (Output):
     * + Hệ thống tìm đúng file JSP giao diện phòng chơi.
     * + Thực hiện lệnh forward(request, response) để hiển thị trang cho người dùng.
     * + Không xảy ra bất kỳ thao tác redirect báo lỗi nào.
     */
    @Test
    public void testDoGet_Step4_3_UserLoggedIn_ShouldForwardToJoinRoom() throws Exception {
        // --- 1. ARRANGE (Chuẩn bị dữ liệu) ---
        doReturn(true).when(servlet).isLoggedIn(request);
        when(request.getRequestDispatcher("/WEB-INF/jsp/room/join-room.jsp")).thenReturn(dispatcher);

        // --- 2. ACT (Thực thi bước 4.2: Trình duyệt gọi GET /find-room) ---
        servlet.doGet(request, response);

        // --- 3. ASSERT (Kiểm tra kết quả bước 4.3) ---
        // Xác nhận hệ thống có gọi tới đúng đường dẫn của file JSP
        verify(request, times(1)).getRequestDispatcher("/WEB-INF/jsp/room/join-room.jsp");

        // Xác nhận lệnh forward được kích hoạt để đẩy giao diện lên trình duyệt
        verify(dispatcher, times(1)).forward(request, response);

        // Đảm bảo không bị vướng lệnh chuyển hướng về trang login
        verify(response, never()).sendRedirect(anyString());
    }
}