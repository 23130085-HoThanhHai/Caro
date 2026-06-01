<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("activeNav", ""); %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Tìm phòng | Gomoku Zen</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css"/>
</head>
<body>
<div class="page">
    <%@ include file="/WEB-INF/jsp/common/header.jspf" %>
    <main class="main" role="main">
        <section class="section">
            <div class="container" style="max-width: 640px;">
                <h2 class="section-title">Tìm / Join Phòng</h2>
                <p class="section-sub" style="margin-bottom: 18px;">Nhập mã phòng để vào trận.</p>
                <!-- -	4.4: Hệ thống hiển thị form “Tìm/Join phòng” -->
                <% if (request.getAttribute("error") != null) { %>
                <div class="card"
                     style="padding:12px; margin-bottom:12px; color:#b42318;"><%= request.getAttribute("error") %>
                </div>
                <% } %>

                <form class="card" style="padding:20px; display:grid; gap:12px;" method="post"
                      action="<%= request.getContextPath() %>/join-room">
                    <label>
                        <div style="font-weight:600; margin-bottom:6px;">Mã phòng</div>
                        <input name="roomCode" required maxlength="12"
                               value="<%= request.getAttribute("roomCode") != null ? request.getAttribute("roomCode") : "" %>"
                               style="width:100%; padding:10px; border:1px solid #ddd; border-radius:8px;"
                               placeholder="Ví dụ: AB12CD"/>
                    </label>

                    <button class="btn btn-primary" type="submit" style="padding:10px 14px;">Vào phòng</button>
                </form>
            </div>
        </section>
    </main>
    <%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
</div>
</body>
</html>
