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

@WebServlet(name = "registerServlet", value = "/register")
public class RegisterServlet extends HttpServlet {
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AuthSession.AUTH_USER) != null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        request.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirm = request.getParameter("confirmPassword");

        if (password == null || !password.equals(confirm)) {
            request.setAttribute("error", "Mật khẩu xác nhận không khớp");
            request.setAttribute("username", username);
            request.setAttribute("email", email);
            request.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(request, response);
            return;
        }

        try {
            AuthUser user = authService.register(username, email, password);
            request.getSession(true).setAttribute(AuthSession.AUTH_USER, user);
            response.sendRedirect(request.getContextPath() + "/index.jsp");
        } catch (AuthException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("username", username);
            request.setAttribute("email", email);
            request.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(request, response);
        } catch (SQLException e) {
            log("Register failed", e);
            request.setAttribute("error", SqlErrors.toUserMessage(e));
            request.setAttribute("username", username);
            request.setAttribute("email", email);
            request.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(request, response);
        }
    }
}
