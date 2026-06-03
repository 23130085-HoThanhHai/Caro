<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("activeNav", ""); %>
<%
    String error = (String) request.getAttribute("error");
    String username = (String) request.getAttribute("username");
    String email = (String) request.getAttribute("email");
    if (username == null) username = "";
    if (email == null) email = "";
%>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Đăng ký | Gomoku Zen</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css" />
</head>
<body>
<div class="page">
    <%@ include file="/WEB-INF/jsp/common/header.jspf" %>

    <main class="main" role="main">
        <section class="section" aria-label="Đăng ký">
            <div class="container section-head">
                <div>
                    <h2 class="section-title">Đăng ký</h2>
                    <p class="section-sub">Tạo tài khoản để chơi online và xếp hạng.</p>
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

                        <form class="auth-form" method="post" action="register" autocomplete="on">
                            <div class="form-row">
                                <label class="form-label" for="username">Username</label>
                                <input class="form-input" type="text" id="username" name="username" value="<%= h(username) %>" required />
                            </div>

                            <div class="form-row">
                                <label class="form-label" for="email">Email</label>
                                <input class="form-input" type="email" id="email" name="email" value="<%= h(email) %>" required />
                            </div>

                            <div class="form-row">
                                <label class="form-label" for="password">Mật khẩu</label>
                                <input class="form-input" type="password" id="password" name="password" required />
                            </div>

                            <div class="form-row">
                                <label class="form-label" for="confirmPassword">Nhập lại mật khẩu</label>
                                <input class="form-input" type="password" id="confirmPassword" name="confirmPassword" required />
                            </div>

                            <button class="btn btn-primary btn-block" type="submit">Tạo tài khoản</button>
                        </form>

                        <div class="auth-divider"><span>hoặc</span></div>
                        <a class="btn btn-outline btn-block" href="auth/google">Đăng ký bằng Google</a>

                        <div class="auth-note">
                            Đã có tài khoản? <a class="link-accent" href="login">Đăng nhập</a>
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
