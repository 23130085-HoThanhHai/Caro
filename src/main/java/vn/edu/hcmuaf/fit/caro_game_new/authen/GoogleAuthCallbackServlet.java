package vn.edu.hcmuaf.fit.demo3.web.auth;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.demo3.config.AppConfig;
import vn.edu.hcmuaf.fit.demo3.model.AuthUser;
import vn.edu.hcmuaf.fit.demo3.service.AuthException;
import vn.edu.hcmuaf.fit.demo3.service.AuthService;
import vn.edu.hcmuaf.fit.demo3.service.GoogleOAuthClient;
import vn.edu.hcmuaf.fit.demo3.service.GoogleUserInfo;
import vn.edu.hcmuaf.fit.demo3.util.WebUrls;
import vn.edu.hcmuaf.fit.demo3.util.SqlErrors;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name = "googleAuthCallbackServlet", value = "/auth/google/callback")
public class GoogleAuthCallbackServlet extends HttpServlet {
    private final GoogleOAuthClient google = new GoogleOAuthClient();
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);

        String code = request.getParameter("code");
        String state = request.getParameter("state");
        String expectedState = session == null ? null : (String) session.getAttribute(AuthSession.GOOGLE_OAUTH_STATE);
        if (session != null) {
            session.removeAttribute(AuthSession.GOOGLE_OAUTH_STATE);
        }

        if (code == null || code.isBlank() || state == null || state.isBlank() || expectedState == null || !expectedState.equals(state)) {
            if (session == null) session = request.getSession(true);
            session.setAttribute(AuthSession.FLASH_ERROR, "Google đăng nhập thất bại (state/code không hợp lệ)");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String clientId = AppConfig.get("google.clientId");
        String clientSecret = AppConfig.get("google.clientSecret");
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            if (session == null) session = request.getSession(true);
            session.setAttribute(AuthSession.FLASH_ERROR, "Chưa cấu hình Google OAuth (google.clientId / google.clientSecret)");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String redirectUri = AppConfig.get("google.redirectUri");
        if (redirectUri == null || redirectUri.isBlank()) {
            redirectUri = WebUrls.absolute(request, "/auth/google/callback");
        }

        try {
            String accessToken = google.exchangeCodeForAccessToken(code, clientId, clientSecret, redirectUri);
            GoogleUserInfo userInfo = google.fetchUserInfo(accessToken);
            AuthUser user = authService.loginWithGoogle(userInfo);
            request.getSession(true).setAttribute(AuthSession.AUTH_USER, user);
            response.sendRedirect(request.getContextPath() + "/index.jsp");
        } catch (IOException | InterruptedException e) {
            log("Google OAuth failed", e);
            if (session == null) session = request.getSession(true);
            session.setAttribute(AuthSession.FLASH_ERROR, "Google đăng nhập thất bại. Vui lòng thử lại.");
            response.sendRedirect(request.getContextPath() + "/login");
        } catch (AuthException e) {
            if (session == null) session = request.getSession(true);
            session.setAttribute(AuthSession.FLASH_ERROR, e.getMessage());
            response.sendRedirect(request.getContextPath() + "/login");
        } catch (SQLException e) {
            log("Google login DB failed", e);
            if (session == null) session = request.getSession(true);
            session.setAttribute(AuthSession.FLASH_ERROR, SqlErrors.toUserMessage(e));
            response.sendRedirect(request.getContextPath() + "/login");
        }
    }
}
