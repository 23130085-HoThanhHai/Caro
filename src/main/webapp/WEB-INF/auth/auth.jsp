<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("activeNav", ""); %>
<%
    String error = (String) request.getAttribute("error");
    String identifier = (String) request.getAttribute("identifier");
    if (identifier == null) identifier = "";
%>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Đăng nhập | Gomoku Zen</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css" />
</head>
<body>
<div class="page">
    <%@ include file="/WEB-INF/jsp/common/header.jspf" %>

    <main class="main" role="main">
        <section class="section" aria-label="Đăng nhập">
            <div class="container section-head">
                <div>
                    <h2 class="section-title">Đăng nhập</h2>
                    <p class="section-sub">Truy cập để tham gia phòng và lưu xếp hạng.</p>
                </div>
                <div class="section-accent" aria-hidden="true"></div>
            </div>

            <div class="container auth-shell">
                <div class="card auth-card">
                    <div class="auth-body">
                        <% if (error != null && !error.isBlank()) { %>
                        <div class="notice" role="alert">
                            <div class="notice-title">Thông báo</div>
                            <div><%= h(error) %></div>
                        </div>
                        <% } %>

                        <form class="auth-form" method="post" action="login" autocomplete="on">
                            <div class="form-row">
                                <label class="form-label" for="identifier">Username hoặc Email</label>
                                <input class="form-input" type="text" id="identifier" name="identifier" value="<%= h(identifier) %>" required />
                            </div>

                            <div class="form-row">
                                <label class="form-label" for="password">Mật khẩu</label>
                                <input class="form-input" type="password" id="password" name="password" required />
                            </div>

                            <button class="btn btn-primary btn-block" type="submit">Đăng nhập</button>
                        </form>

                        <div class="auth-divider"><span>hoặc</span></div>
                        <a class="btn btn-outline btn-block" href="auth/google">Tiếp tục với Google</a>

                        <div class="auth-note">
                            Chưa có tài khoản? <a class="link-accent" href="register">Đăng ký</a>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    </main>

    <%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
</div>
</body>
</html>
