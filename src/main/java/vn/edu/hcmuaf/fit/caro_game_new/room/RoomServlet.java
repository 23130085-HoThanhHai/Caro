
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.caro_game_new.model.ChatMessage;
import vn.edu.hcmuaf.fit.caro_game_new.service.ChatService;
import vn.edu.hcmuaf.fit.demo3.model.AuthUser;
import vn.edu.hcmuaf.fit.demo3.model.Room;
import vn.edu.hcmuaf.fit.demo3.service.RoomService;
import vn.edu.hcmuaf.fit.demo3.web.auth.AuthSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
// -	4.10: Server gửi lệnh chuyển hướng (Redirect)
//  Sau khi xử lý Join phòng thành công, server không render view ngay lập tức
// mà gửi trả về một response mang mã HTTP 302 Redirect, hướng sang URL GET /room?code=<roomCode>.
@WebServlet(name = "roomServlet", value = "/room")
public class RoomServlet extends HttpServlet {
    private final RoomService roomService = new RoomService();
    private final ChatService chatService = new ChatService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getSession(false) == null || !(request.getSession(false).getAttribute(AuthSession.AUTH_USER) instanceof AuthUser authUser)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String roomCode = request.getParameter("code");
        if (roomCode == null || roomCode.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/find-room");
            return;
        }

        try {
            Optional<Room> roomOpt = roomService.getRoomByCode(roomCode.trim().toUpperCase());
            if (roomOpt.isEmpty()) {
                request.setAttribute("error", "Không tìm thấy phòng");
                request.getRequestDispatcher("/WEB-INF/jsp/room/join-room.jsp").forward(request, response);
                return;
            }

            Room room = roomOpt.get();

            List<ChatMessage> messages = chatService.loadMessages(room.getId());
            request.setAttribute("messages", messages);


            request.setAttribute("room", room);
            request.setAttribute("currentUserId", authUser.getId());
            request.getRequestDispatcher("/WEB-INF/jsp/room/room.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Load room failed", e);
            request.setAttribute("error", "Không thể tải phòng");
            request.getRequestDispatcher("/WEB-INF/jsp/room/join-room.jsp").forward(request, response);
        }
    }
}
