package vn.edu.hcmuaf.fit.demo3.web.auth;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.demo3.config.AppConfig;
import vn.edu.hcmuaf.fit.demo3.util.WebUrls;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@WebServlet(name = "googleAuthStartServlet", value = "/auth/google")
public class GoogleAuthStartServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientId = AppConfig.get("google.clientId");
        String clientSecret = AppConfig.get("google.clientSecret");
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            HttpSession session = request.getSession(true);
            session.setAttribute(AuthSession.FLASH_ERROR, "Chưa cấu hình Google OAuth (google.clientId / google.clientSecret)");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String redirectUri = AppConfig.get("google.redirectUri");
        if (redirectUri == null || redirectUri.isBlank()) {
            redirectUri = WebUrls.absolute(request, "/auth/google/callback");
        }

        String state = UUID.randomUUID().toString();
        request.getSession(true).setAttribute(AuthSession.GOOGLE_OAUTH_STATE, state);

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&response_type=code"
                + "&scope=" + enc("openid email profile")
                + "&state=" + enc(state);

        response.sendRedirect(authUrl);
    }

    private static String enc(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }
}
