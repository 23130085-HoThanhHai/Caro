import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.demo3.model.AuthUser;
import vn.edu.hcmuaf.fit.demo3.model.Room;
import vn.edu.hcmuaf.fit.demo3.service.RoomException;
import vn.edu.hcmuaf.fit.demo3.service.RoomService;
import vn.edu.hcmuaf.fit.demo3.web.auth.AuthSession;

import java.io.IOException;
import java.sql.SQLException;
// -	4.2: Trình duyệt gửi HTTP GET /find-room
@WebServlet(name = "joinRoomServlet", urlPatterns = {"/find-room", "/join-room"})
public class JoinRoomServlet extends HttpServlet {
    private final RoomService roomService = new RoomService();

    @Override
    // -	4.3: Server kiểm tra đăng nhập và forward trang nhập mã phòng
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isLoggedIn(request)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/jsp/room/join-room.jsp").forward(request, response);
    }