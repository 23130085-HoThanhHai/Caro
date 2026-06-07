
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class GoogleOAuthClient {
    private static final URI TOKEN_URI = URI.create("https://oauth2.googleapis.com/token");
    private static final URI USERINFO_URI = URI.create("https://www.googleapis.com/oauth2/v3/userinfo");

    private final HttpClient http;
    private final ObjectMapper mapper;

    public GoogleOAuthClient() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    public String exchangeCodeForAccessToken(String code, String clientId, String clientSecret, String redirectUri)
            throws IOException, InterruptedException {
        String body = form(
                "code", code,
                "client_id", clientId,
                "client_secret", clientSecret,
                "redirect_uri", redirectUri,
                "grant_type", "authorization_code"
        );

        HttpRequest request = HttpRequest.newBuilder(TOKEN_URI)
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Token exchange failed (HTTP " + response.statusCode() + ")");
        }

        JsonNode node = mapper.readTree(response.body());
        JsonNode accessToken = node.get("access_token");
        if (accessToken == null || accessToken.asText().isBlank()) {
            throw new IOException("Token exchange response missing access_token");
        }
        return accessToken.asText();
    }

    public GoogleUserInfo fetchUserInfo(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(USERINFO_URI)
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Userinfo fetch failed (HTTP " + response.statusCode() + ")");
        }

        JsonNode node = mapper.readTree(response.body());
        String email = textOrNull(node.get("email"));
        String name = textOrNull(node.get("name"));
        String picture = textOrNull(node.get("picture"));
        return new GoogleUserInfo(email, name, picture);
    }

    private static String form(String... kv) {
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("form must have even number of strings");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) sb.append('&');
            sb.append(encode(kv[i])).append('=').append(encode(kv[i + 1]));
        }
        return sb.toString();
    }

    private static String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String textOrNull(JsonNode node) {
        if (node == null) return null;
        String t = node.asText();
        return t == null || t.isBlank() ? null : t;
    }
}
