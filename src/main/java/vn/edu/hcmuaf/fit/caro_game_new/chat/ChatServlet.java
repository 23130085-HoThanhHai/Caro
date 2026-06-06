package vn.edu.hcmuaf.fit.caro_game_new.chat;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import vn.edu.hcmuaf.fit.caro_game_new.model.AuthUser;
import vn.edu.hcmuaf.fit.caro_game_new.service.ChatService;
import  vn.edu.hcmuaf.fit.demo3.web.auth.AuthSession;

import java.io.IOException;

@WebServlet("/chat/send")
public class ChatServlet extends HttpServlet {

    private final ChatService chatService =
            new ChatService();

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        try {

            HttpSession session =
                    request.getSession(false);

            if (session == null) {
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Bạn chưa đăng nhập");
                return;
            }

            AuthUser user =
                    (AuthUser) session.getAttribute(
                            AuthSession.AUTH_USER);

            if (user == null) {
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Bạn chưa đăng nhập");
                return;
            }

            long roomId = Long.parseLong(
                    request.getParameter("roomId"));

            String text =
                    request.getParameter("message");

            chatService.sendMessage(
                    user,
                    roomId,
                    text);

            String referer =
                    request.getHeader("Referer");

            if (referer != null) {
                response.sendRedirect(referer);
            } else {
                response.sendRedirect(
                        request.getContextPath());
            }

        } catch (Exception e) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage());
        }
    }
}