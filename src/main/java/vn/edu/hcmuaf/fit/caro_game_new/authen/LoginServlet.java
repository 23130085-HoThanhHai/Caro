package vn.edu.hcmuaf.fit.demo3.web.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.demo3.model.AuthUser;
import vn.edu.hcmuaf.fit.demo3.service.AuthException;
import vn.edu.hcmuaf.fit.demo3.service.AuthService;
import vn.edu.hcmuaf.fit.demo3.util.SqlErrors;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name = "loginServlet", value = "/login")
public class LoginServlet extends HttpServlet {
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AuthSession.AUTH_USER) != null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        if (session != null) {
            Object flash = session.getAttribute(AuthSession.FLASH_ERROR);
            if (flash instanceof String msg && !msg.isBlank()) {
                request.setAttribute("error", msg);
            }
            session.removeAttribute(AuthSession.FLASH_ERROR);
        }

        request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String identifier = request.getParameter("identifier");
        String password = request.getParameter("password");

        try {
            AuthUser user = authService.login(identifier, password);
            request.getSession(true).setAttribute(AuthSession.AUTH_USER, user);
            response.sendRedirect(request.getContextPath() + "/index.jsp");
        } catch (AuthException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("identifier", identifier);
            request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Login failed", e);
            request.setAttribute("error", SqlErrors.toUserMessage(e));
            request.setAttribute("identifier", identifier);
            request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(request, response);
        }
    }
}
