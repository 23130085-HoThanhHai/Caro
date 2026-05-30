<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("activeNav", ""); %>
<!doctype html>
<html lang="vi">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Tạo phòng | Gomoku Zen</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css" />
</head>
<body>
  <div class="page">
    <%@ include file="/WEB-INF/jsp/common/header.jspf" %>
    <main class="main" role="main">
      <section class="section">
        <div class="container" style="max-width: 640px;">
          <h2 class="section-title">Tạo Phòng</h2>
          <p class="section-sub" style="margin-bottom: 18px;">Tạo phòng đấu và chia sẻ mã cho bạn bè.</p>

          <% if (request.getAttribute("error") != null) { %>
            <div class="card" style="padding:12px; margin-bottom:12px; color:#b42318;"><%= request.getAttribute("error") %></div>
          <% } %>
            <!-- Bước 3 — Nhập tên phòng + kích thước, bấm "Tạo phòng"
 -->
          <form class="card" style="padding:20px; display:grid; gap:12px;" method="post" action="<%= request.getContextPath() %>/create-room">
            <label>
              <div style="font-weight:600; margin-bottom:6px;">Tên phòng</div>
              <input name="roomName" required maxlength="100" value="<%= request.getAttribute("roomName") != null ? request.getAttribute("roomName") : "" %>"
                     style="width:100%; padding:10px; border:1px solid #ddd; border-radius:8px;" placeholder="Ví dụ: Phòng của Minh" />
            </label>

            <label>
              <div style="font-weight:600; margin-bottom:6px;">Kích thước bàn cờ</div>
              <select name="boardSize" style="width:100%; padding:10px; border:1px solid #ddd; border-radius:8px;">
                <option value="13">13 x 13</option>
                <option value="15" selected>15 x 15</option>
                <option value="19">19 x 19</option>
              </select>
            </label>
<!-- kết thúc bước 3 -->
            <button class="btn btn-primary" type="submit" style="padding:10px 14px;">Tạo phòng</button>
          </form>
        </div>
      </section>
    </main>
    <%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
  </div>
</body>
</html>
