<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("activeNav", ""); %>
<!doctype html>
<html lang="vi">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Chơi Offline | Gomoku Zen</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css" />
  <style>
    .container-narrow {
      max-width: 600px;
      margin: 0 auto;
      padding: 40px 20px;
    }
    .game-modes {
      display: grid;
      gap: 16px;
      margin-bottom: 24px;
    }
    .mode-selector {
      padding: 20px;
      border: 2px solid rgba(16,24,40,0.1);
      border-radius: 12px;
      cursor: pointer;
      transition: all 0.2s ease;
      text-align: center;
      background: rgba(255,255,255,0.5);
    }
    .mode-selector:hover {
      border-color: var(--accent);
      background: rgba(255,255,255,0.8);
      transform: translateY(-2px);
    }
    .mode-selector.active {
      border-color: var(--accent);
      background: rgba(180,83,9,0.08);
    }
    .mode-selector h3 {
      font-size: 16px;
      font-weight: 700;
      margin: 0 0 8px 0;
      color: #222;
    }
    .mode-selector p {
      font-size: 13px;
      color: rgba(16,24,40,0.6);
      margin: 0;
    }
    .difficulty-group {
      margin-top: 24px;
      padding: 20px;
      background: rgba(16,24,40,0.02);
      border-radius: 12px;
      border: 1px solid rgba(16,24,40,0.1);
    }
    .difficulty-group h4 {
      font-size: 13px;
      font-weight: 700;
      text-transform: uppercase;
      color: rgba(16,24,40,0.6);
      margin: 0 0 14px 0;
      letter-spacing: 0.5px;
    }
    .difficulty-options {
      display: grid;
      gap: 10px;
    }
    .difficulty-btn {
      padding: 12px;
      border: 1.5px solid rgba(16,24,40,0.15);
      background: white;
      border-radius: 8px;
      cursor: pointer;
      text-align: center;
      font-weight: 600;
      font-size: 13px;
      transition: all 0.2s ease;
    }
    .difficulty-btn:hover {
      border-color: rgba(16,24,40,0.3);
    }
    .difficulty-btn.active {
      font-weight: 700;
    }
    .difficulty-btn.easy.active {
      background: rgba(46,204,113,0.2);
      border-color: #27ae60;
      color: #27ae60;
    }
    .difficulty-btn.medium.active {
      background: rgba(241,196,15,0.2);
      border-color: #f39c12;
      color: #f39c12;
    }
    .difficulty-btn.hard.active {
      background: rgba(231,76,60,0.2);
      border-color: #e74c3c;
      color: #e74c3c;
    }
    .board-size-group {
      margin-top: 12px;
      padding: 16px;
      background: rgba(16,24,40,0.02);
      border-radius: 8px;
    }
    .board-size-group h4 {
      font-size: 12px;
      font-weight: 700;
      text-transform: uppercase;
      color: rgba(16,24,40,0.6);
      margin: 0 0 10px 0;
      letter-spacing: 0.5px;
    }
    .size-options {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 8px;
    }
    .size-btn {
      padding: 8px;
      border: 1.5px solid rgba(16,24,40,0.15);
      background: white;
      border-radius: 6px;
      cursor: pointer;
      font-weight: 600;
      font-size: 12px;
      transition: all 0.2s ease;
    }
    .size-btn:hover {
      border-color: rgba(16,24,40,0.3);
    }
    .size-btn.active {
      background: var(--accent);
      color: white;
      border-color: var(--accent);
      font-weight: 700;
    }
    .start-btn {
      width: 100%;
      padding: 12px;
      background: var(--accent);
      color: white;
      border: none;
      border-radius: 10px;
      font-weight: 700;
      font-size: 14px;
      cursor: pointer;
      margin-top: 20px;
      transition: all 0.2s ease;
    }
    .start-btn:hover {
      opacity: 0.9;
      transform: translateY(-2px);
    }
  </style>
</head>
<body>
  <div class="page">
    <%@ include file="/WEB-INF/jsp/common/header.jspf" %>

    <main class="main" role="main">
      <section class="section" aria-label="Chơi Offline">
        <div class="container-narrow">
          <div class="section-head">
            <h2 class="section-title">Chơi Với Máy</h2>
            <p class="section-sub">Thiết lập độ khó và kích thước trước khi vào trận</p>
          </div>

          <div class="card" style="padding: 30px;">
            <div class="difficulty-group">
              <h4>Chọn độ khó</h4>
              <div class="difficulty-options">
                <button class="difficulty-btn easy active" onclick="selectDifficulty('easy', this)">
                  🤖 Dễ - Máy học đi
                </button>
                <button class="difficulty-btn medium" onclick="selectDifficulty('medium', this)">
                  ⚔️ Trung Bình - Chuẩn
                </button>
                <button class="difficulty-btn hard" onclick="selectDifficulty('hard', this)">
                  🧠 Khó - Máy rất thông minh
                </button>
              </div>
            </div>

            <div class="board-size-group">
              <h4>Kích thước bàn cờ</h4>
              <div class="size-options">
                <button class="size-btn" onclick="selectSize(13, this)">13×13</button>
                <button class="size-btn active" onclick="selectSize(15, this)">15×15</button>
                <button class="size-btn" onclick="selectSize(19, this)">19×19</button>
              </div>
            </div>

            <button class="start-btn" onclick="startGame()">Bắt đầu chơi →</button>
          </div>

          <div style="text-align: center; margin-top: 20px;">
            <a class="link-accent" href="<%= request.getContextPath() %>/index.jsp">← Về sảnh</a>
          </div>
        </div>
      </section>
    </main>

    <%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
  </div>

  <script>
    let selectedDifficulty = 'easy';
    let selectedSize = 15;

    function selectDifficulty(diff, btn) {
      document.querySelectorAll('.difficulty-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      selectedDifficulty = diff;
    }

    function selectSize(size, btn) {
      document.querySelectorAll('.size-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      selectedSize = size;
    }

    function startGame() {
      const basePath = '<%= request.getAttribute("offlineBasePath") != null ? request.getAttribute("offlineBasePath") : (request.getContextPath() + "/offline-bot") %>';
      const url = basePath + '?action=new&mode=bot&difficulty=' + selectedDifficulty + '&size=' + selectedSize;
      window.location.href = url;
    }
  </script>
</body>
</html>
