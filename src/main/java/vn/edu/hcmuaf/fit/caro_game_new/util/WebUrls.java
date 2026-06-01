package vn.edu.hcmuaf.fit.demo3.util;

import jakarta.servlet.http.HttpServletRequest;

public final class WebUrls {
    private WebUrls() {
    }

    public static String baseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        String context = request.getContextPath();

        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);

        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(host);
        if (!defaultPort) {
            sb.append(":").append(port);
        }
        sb.append(context);
        return sb.toString();
    }

    public static String absolute(HttpServletRequest request, String path) {
        String p = path == null ? "" : path.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return baseUrl(request) + p;
    }
}
