package vn.edu.hcmuaf.fit.demo3.web.room;

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

@WebServlet(name = "createRoomServlet", value = "/create-room")
public class CreateRoomServlet extends HttpServlet {
    private final RoomService roomService = new RoomService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isLoggedIn(request)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/jsp/room/create-room.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        AuthUser authUser = request.getSession(false) == null
                ? null
                : (AuthUser) request.getSession(false).getAttribute(AuthSession.AUTH_USER);

        String roomName = request.getParameter("roomName");
        String boardSizeRaw = request.getParameter("boardSize");

        int boardSize;
        try {
            boardSize = Integer.parseInt(boardSizeRaw);
        } catch (Exception ignored) {
            boardSize = 15;
        }

        try {
            Room room = roomService.createRoom(authUser, roomName, boardSize);
            response.sendRedirect(request.getContextPath() + "/room?code=" + room.getRoomCode());
        } catch (RoomException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("roomName", roomName);
            request.setAttribute("boardSize", boardSize);
            request.getRequestDispatcher("/WEB-INF/jsp/room/create-room.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Create room failed", e);
            request.setAttribute("error", "Không thể tạo phòng lúc này, vui lòng thử lại");
            request.setAttribute("roomName", roomName);
            request.setAttribute("boardSize", boardSize);
            request.getRequestDispatcher("/WEB-INF/jsp/room/create-room.jsp").forward(request, response);
        }
    }

    private boolean isLoggedIn(HttpServletRequest request) {
        return request.getSession(false) != null
                && request.getSession(false).getAttribute(AuthSession.AUTH_USER) instanceof AuthUser;
    }
}
