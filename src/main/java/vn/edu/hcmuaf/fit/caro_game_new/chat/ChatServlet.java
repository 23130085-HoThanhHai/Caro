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

    private final ChatService chatService = new ChatService();

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        try {

            AuthUser user =
                    (AuthUser) request.getSession()
                            .getAttribute(
                                    AuthSession.AUTH_USER);

            long roomId =
                    Long.parseLong(
                            request.getParameter("roomId"));

            String text =
                    request.getParameter("message");

            chatService.sendMessage(
                    user,
                    roomId,
                    text);

            response.sendRedirect(
                    request.getHeader("Referer"));

        } catch (Exception e) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage());
        }
    }
}