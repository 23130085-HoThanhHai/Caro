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
    .offline-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 20px;
    }
    .game-wrapper {
      display: grid;
      grid-template-columns: 1fr 320px;
      gap: 24px;
      align-items: start;
    }
    @media (max-width: 900px) {
      .game-wrapper {
        grid-template-columns: 1fr;
      }
      .game-sidebar {
        order: -1;
      }
    }
    .game-board-container {
      background: linear-gradient(135deg, rgba(180,83,9,0.04) 0%, rgba(180,83,9,0.08) 100%);
      padding: 16px;
      border-radius: 16px;
      border: 1px solid rgba(180,83,9,0.15);
    }
    .game-board {
      display: grid;
      gap: 0;
      background: rgba(255,255,255,0.6);
      padding: 12px;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(16,24,40,0.08);
    }
    .board-cell {
      width: 42px;
      height: 42px;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 1px solid rgba(16,24,40,0.1);
      cursor: pointer;
      position: relative;
      background: rgba(255,255,255,0.9);
      font-size: 20px;
      font-weight: bold;
      transition: all 0.15s ease;
    }
    .board-cell:hover {
      background: rgba(180,83,9,0.1);
      transform: scale(1.05);
    }
    .board-cell.disabled {
      cursor: not-allowed;
      opacity: 0.6;
    }
    .stone {
      width: 30px;
      height: 30px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      animation: stonePlace 0.3s ease-out;
      box-shadow: 0 2px 6px rgba(0,0,0,0.15);
    }
    @keyframes stonePlace {
      0% {
        transform: scale(0.3) rotate(0deg);
        opacity: 0;
      }
      70% {
        transform: scale(1.1);
      }
      100% {
        transform: scale(1) rotate(0deg);
        opacity: 1;
      }
    }
    .stone-player {
      background: linear-gradient(135deg, #1abc9c 0%, #16a085 100%);
      color: white;
    }
    .stone-bot {
      background: linear-gradient(135deg, #2c3e50 0%, #1a252f 100%);
      color: white;
    }
    .game-sidebar {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .game-info-card {
      background: rgba(180,83,9,0.06);
      border: 1px solid rgba(180,83,9,0.2);
      border-radius: 12px;
      padding: 16px;
    }
    .info-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 0;
      font-size: 14px;
    }
    .info-label {
      color: rgba(16,24,40,0.6);
      font-weight: 500;
    }
    .info-value {
      font-weight: 700;
      color: var(--accent);
    }
    .difficulty-badge {
      display: inline-block;
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
      text-transform: uppercase;
    }
    .difficulty-easy {
      background: rgba(46,204,113,0.2);
      color: #27ae60;
    }
    .difficulty-medium {
      background: rgba(241,196,15,0.2);
      color: #f39c12;
    }
    .difficulty-hard {
      background: rgba(231,76,60,0.2);
      color: #e74c3c;
    }
    .move-history {
      background: rgba(16,24,40,0.02);
      border-radius: 8px;
      max-height: 200px;
      overflow-y: auto;
      padding: 8px;
    }
    .history-item {
      padding: 4px 8px;
      font-size: 12px;
      border-radius: 4px;
      margin: 2px 0;
      background: rgba(16,24,40,0.05);
      font-family: monospace;
    }
    .history-item.player {
      border-left: 3px solid #1abc9c;
      padding-left: 6px;
    }
    .history-item.bot {
      border-left: 3px solid #2c3e50;
      padding-left: 6px;
    }
    .game-status {
      font-size: 14px;
      color: rgba(16,24,40,0.7);
      padding: 12px;
      background: rgba(16,24,40,0.02);
      border-radius: 8px;
      text-align: center;
    }
    .game-status.finished {
      font-weight: 700;
      color: white;
      background: var(--accent);
    }
    .btn-row {
      display: flex;
      gap: 8px;
    }
    .btn-resign, .btn-undo, .btn-new {
      flex: 1;
      padding: 9px 12px;
      border: none;
      border-radius: 10px;
      font-weight: 600;
      font-size: 13px;
      cursor: pointer;
      transition: all 0.2s ease;
    }
    .btn-resign {
      background: rgba(220,53,69,0.15);
      border: 1px solid rgba(220,53,69,0.3);
      color: #dc3545;
    }
    .btn-resign:hover {
      background: rgba(220,53,69,0.25);
    }
    .btn-resign:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    .btn-undo {
      background: rgba(52,152,219,0.15);
      border: 1px solid rgba(52,152,219,0.3);
      color: #3498db;
    }
    .btn-undo:hover {
      background: rgba(52,152,219,0.25);
    }
    .btn-undo:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    .btn-new {
      background: var(--accent);
      color: white;
    }
    .btn-new:hover {
      opacity: 0.9;
    }
  </style>
</head>
<body>
  <div class="page">
    <%@ include file="/WEB-INF/jsp/common/header.jspf" %>

    <main class="main" role="main">
      <section class="section" aria-label="Chơi Offline">
        <div class="container offline-container">
          <%
            vn.edu.hcmuaf.fit.demo3.model.OfflineGame game = (vn.edu.hcmuaf.fit.demo3.model.OfflineGame) request.getAttribute("game");
            if (game != null) {
              boolean gameFinished = game.getState().toString().equals("FINISHED");
              int moveCount = game.getMoves().size();
          %>
          <div class="section-head" style="margin-bottom: 20px;">
            <h2 class="section-title"><%= game != null && game.getGameMode().toString().equals("TWO_PLAYERS") ? "Chơi 2 Người Offline" : "Chơi Với Máy" %></h2>
          </div>

          <div class="game-wrapper">
            <div class="game-board-container">
              <div id="gameBoard" class="game-board" style="grid-template-columns: repeat(<%= game.getBoardSize() %>, 1fr);">
                <%
                  byte[][] board = game.getBoard();
                  for (int y = 0; y < game.getBoardSize(); y++) {
                    for (int x = 0; x < game.getBoardSize(); x++) {
                      byte cell = board[x][y];
                      String cellContent = "";
                      if (cell == 1) {
                        cellContent = "<div class='stone stone-player'>●</div>";
                      } else if (cell == 2) {
                        cellContent = "<div class='stone stone-bot'>●</div>";
                      }
                      String disabledClass = gameFinished ? "disabled" : "";
                %>
                <div class="board-cell <%= disabledClass %>" data-x="<%= x %>" data-y="<%= y %>">
                  <%= cellContent %>
                </div>
                <%
                    }
                  }
                %>
              </div>
            </div>

            <div class="game-sidebar">
              <div class="game-info-card">
                <div class="info-row">
                  <span class="info-label">Kích thước</span>
                  <span class="info-value"><%= game.getBoardSize() %>×<%= game.getBoardSize() %></span>
                </div>
                <div class="info-row">
                  <span class="info-label">Chế độ</span>
                  <span class="info-value"><%= game.getGameMode().toString().equals("TWO_PLAYERS") ? "2 Người" : "Đấu Máy" %></span>
                </div>
                <% if (!game.getGameMode().toString().equals("TWO_PLAYERS")) { %>
                <div class="info-row">
                  <span class="info-label">Độ khó</span>
                  <span><span class="difficulty-badge difficulty-<%= game.getDifficulty().toString().toLowerCase() %>"><%= game.getDifficulty() %></span></span>
                </div>
                <% } %>
                <div class="info-row">
                  <span class="info-label">Tổng nước</span>
                  <span class="info-value" id="totalMoves"><%= moveCount %></span>
                </div>
              </div>

              <div class="game-info-card">
                <div id="gameStatus" class="game-status <%= gameFinished ? "finished" : "" %>">
                  <% if (gameFinished) { %>
                    <% if ("P1_WIN".equals(game.getResult().toString())) { %>
                      🎉 <%= game.getGameMode().toString().equals("TWO_PLAYERS") ? "Người chơi 1 thắng!" : "Bạn thắng!" %>
                    <% } else if ("P2_WIN".equals(game.getResult().toString())) { %>
                      😔 <%= game.getGameMode().toString().equals("TWO_PLAYERS") ? "Người chơi 2 thắng!" : "Máy thắng" %>
                    <% } else { %>
                      🤝 Hòa
                    <% } %>
                  <% } else { %>
                    <%= game.getGameMode().toString().equals("TWO_PLAYERS")
                            ? (game.getCurrentPlayer() == 1 ? "Lượt người chơi 1 (Xanh)" : "Lượt người chơi 2 (Đen)")
                            : (game.getCurrentPlayer() == 1 ? "Lượt của bạn" : "Máy đang suy nghĩ...") %>
                  <% } %>
                </div>
              </div>

              <% if (moveCount > 0) { %>
              <div class="game-info-card">
                <div style="font-size: 12px; color: rgba(16,24,40,0.6); margin-bottom: 8px; font-weight: 600;">Lịch sử nước đi:</div>
                <div id="moveHistory" class="move-history">
                  <%
                    int moveNo = 1;
                    java.util.List<int[]> moves = game.getMoves();
                    for (int i = 0; i < moves.size(); i++) {
                      int[] move = moves.get(i);
                      String player = game.getGameMode().toString().equals("TWO_PLAYERS")
                              ? ((i % 2 == 0) ? "Người chơi 1" : "Người chơi 2")
                              : ((i % 2 == 0) ? "Bạn" : "Máy");
                      String histClass = (i % 2 == 0) ? "player" : "bot";
                      char col = (char) ('A' + move[0]);
                      int row = move[1] + 1;
                  %>
                  <div class="history-item <%= histClass %>">#<%= moveNo %>: <%= player %> - <%= col %><%= row %></div>
                  <%
                      moveNo++;
                    }
                  %>
                </div>
              </div>
              <% } %>

              <div class="btn-row">
                <button id="undoBtn" class="btn-undo" onclick="undoMove()" <%= gameFinished || (game.getGameMode().toString().equals("TWO_PLAYERS") ? moveCount < 1 : moveCount < 2) ? "disabled" : "" %>>↶ Hoàn tác</button>
                <button id="resignBtn" class="btn-resign" onclick="resign()" <%= gameFinished ? "disabled" : "" %>>Thua</button>
              </div>

              <div class="game-info-card">
                <div style="font-size: 12px; color: rgba(16,24,40,0.6); margin-bottom: 8px; font-weight: 600;">Chơi lại nhanh:</div>
                <div style="display:grid; gap:8px;">
                  <% if (game.getGameMode().toString().equals("VS_BOT")) { %>
                  <select id="quickDifficulty" style="padding:8px; border-radius:8px; border:1px solid rgba(16,24,40,0.2);">
                    <option value="easy" <%= game.getDifficulty().toString().equals("EASY") ? "selected" : "" %>>Dễ</option>
                    <option value="medium" <%= game.getDifficulty().toString().equals("MEDIUM") ? "selected" : "" %>>Trung bình</option>
                    <option value="hard" <%= game.getDifficulty().toString().equals("HARD") ? "selected" : "" %>>Khó</option>
                  </select>
                  <% } %>
                  <select id="quickSize" style="padding:8px; border-radius:8px; border:1px solid rgba(16,24,40,0.2);">
                    <option value="13" <%= game.getBoardSize() == 13 ? "selected" : "" %>>13x13</option>
                    <option value="15" <%= game.getBoardSize() == 15 ? "selected" : "" %>>15x15</option>
                    <option value="19" <%= game.getBoardSize() == 19 ? "selected" : "" %>>19x19</option>
                  </select>
                  <button class="btn-new" type="button" onclick="startNewWithOptions()">Ván mới</button>
                </div>
              </div>
              <a class="btn btn-ghost" href="<%= request.getContextPath() %>/index.jsp" style="width: 100%; text-align: center; padding: 11px 14px;">Về sảnh</a>
            </div>
          </div>

          <% } else { %>
          <div class="card" style="padding: 40px; text-align: center;">
            <p style="margin-bottom: 20px;">Không tìm thấy ván chơi</p>
            <a class="btn btn-primary" href="<%= request.getContextPath() %>/offline-play?action=new">Bắt đầu ván mới</a>
          </div>
          <% } %>
        </div>
      </section>
    </main>

    <%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
  </div>

  <script>
    const gameId = '<%= game != null ? game.getGameId() : "" %>';
    const contextPath = '<%= request.getContextPath() %>';
      const offlineBasePath = '<%= request.getAttribute("offlineBasePath") != null ? request.getAttribute("offlineBasePath") : (request.getContextPath() + "/offline-play") %>';
    const boardSize = <%= game != null ? game.getBoardSize() : 15 %>;
    let gameData = {
      gameId: gameId,
      boardSize: boardSize,
      difficulty: '<%= game != null ? game.getDifficulty() : "MEDIUM" %>',
      gameMode: '<%= game != null ? game.getGameMode() : "VS_BOT" %>',
      state: '<%= game != null ? game.getState() : "IN_PROGRESS" %>',
      result: '<%= game != null ? game.getResult() : "NONE" %>',
      currentPlayer: <%= game != null ? game.getCurrentPlayer() : 1 %>,
      board: [
        <% if (game != null) {
             byte[][] boardInit = game.getBoard();
             for (int y = 0; y < game.getBoardSize(); y++) { %>
          [<% for (int x = 0; x < game.getBoardSize(); x++) { %><%= boardInit[x][y] %><%= x < game.getBoardSize() - 1 ? "," : "" %><% } %>]<%= y < game.getBoardSize() - 1 ? "," : "" %>
        <%   }
           } %>
      ],
      moves: [
        <% if (game != null) {
             java.util.List<int[]> initialMoves = game.getMoves();
             for (int i = 0; i < initialMoves.size(); i++) {
               int[] m = initialMoves.get(i); %>
          [<%= m[0] %>,<%= m[1] %>]<%= i < initialMoves.size() - 1 ? "," : "" %>
        <%   }
           } %>
      ]
    };
    let isBusy = false;

    document.addEventListener('DOMContentLoaded', () => {
      bindBoardClicks();
    });

    function bindBoardClicks() {
      const board = document.getElementById('gameBoard');
      if (!board) return;
        board.addEventListener('click', (e) => {
        // [UC-05.1.1]: Người chơi chọn một ô trống trên bàn cờ bằng cách click chuột vào các ô có thuộc tính board-cell. [cite: 5]
        const cell = e.target.closest('.board-cell');
        if (!cell) return;
        // [UC-05.1.2]: Hệ thống xác định vị trí của ô thông qua các thuộc tính tọa độ data-x và data-y. [cite: 5]
        const x = Number(cell.dataset.x);
        const y = Number(cell.dataset.y);
        makeMove(x, y, gameId);
      });
    }

    async function makeMove(x, y, gId) {
      if (gameData.state === 'FINISHED') {
        alert('Ván đấu đã kết thúc');
        return;
      }
      if (isBusy) return;
      if (gameData.gameMode === 'VS_BOT' && gameData.currentPlayer !== 1) return;
      // [UC-05.1.3]: Hệ thống kiểm tra ô chọn hiện tại đang trống (board[y][x] == 0) phía Client. [cite: 5]
      if (gameData.board[y][x] !== 0) return;

      try {
        isBusy = true;
      // [UC-05.1.4]: Hệ thống gửi yêu cầu xử lý nước đi đến máy chủ thông qua phương thức POST. [cite: 5]
      const res = await fetch(offlineBasePath, {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: 'action=move&gameId=' + gId + '&x=' + x + '&y=' + y
        });

        if (!res.ok) throw new Error('Network error');
        const data = await res.json();

        if (!data.success) {
          alert('Lỗi: ' + (data.error || 'Không biết'));
          return;
        }
        updateGameUI(data.game);
      } catch (e) {
        console.error(e);
        alert('Lỗi kết nối');
      } finally {
        isBusy = false;
      }
    }

    async function resign() {
    // [UC-05.7.1]: Người chơi nhấn nút "Thua". [cite: 5]
    // (Logic gửi request POST action=resign)
      const msg = gameData.gameMode === 'TWO_PLAYERS'
              ? ('Bạn chắc chứ? ' + (gameData.currentPlayer === 1 ? 'Người chơi 2' : 'Người chơi 1') + ' sẽ thắng.')
              : 'Bạn chắc chứ? Máy sẽ thắng.';
      if (!confirm(msg)) return;
      if (isBusy) return;
      isBusy = true;
      fetch(offlineBasePath, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'action=resign&gameId=' + gameId
      }).then(r => r.json()).then(data => {
        if (data.success) updateGameUI(data.game);
      }).finally(() => {
        isBusy = false;
      });
    }

    async function undoMove() {
    // [UC-05.6.1]: Người chơi nhấn nút "Hoàn tác". [cite: 5]
    // (Logic gửi request POST action=undo)
      const undoMessage = gameData.gameMode === 'TWO_PLAYERS'
              ? 'Hoàn tác nước đi gần nhất?'
              : 'Hoàn tác lượt vừa đánh (bạn + máy)?';
      if (!confirm(undoMessage)) return;
      if (isBusy) return;
      isBusy = true;
      fetch(offlineBasePath, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'action=undo&gameId=' + gameId
      }).then(r => r.json()).then(data => {
        if (!data.success) {
          alert('Lỗi: ' + (data.error || 'Không thể hoàn tác'));
          return;
        }
        updateGameUI(data.game);
      }).finally(() => {
        isBusy = false;
      });
    }

    function updateGameUI(nextGame) {
      const prevGame = gameData;
      gameData = nextGame;
    // [UC-05.1.8]: Hệ thống cập nhật giao diện người dùng. [cite: 5]
    // [UC-05.1.8.1]: Hiển thị quân cờ tương ứng (stone-player hoặc stone-bot). [cite: 5]
    renderBoard(prevGame, nextGame);
    // [UC-05.1.8.3]: Cập nhật thông tin lượt đi tiếp theo hoặc trạng thái thắng/thua. [cite: 5]
    renderStatus();
    // [UC-05.1.8.2]: Cập nhật danh sách lịch sử nước đi trực quan. [cite: 5]
    renderMoves();
      updateButtons();
    }

    function renderBoard(prevGame, nextGame) {
      const boardCells = document.querySelectorAll('.board-cell');
      boardCells.forEach(cell => {
        const x = Number(cell.dataset.x);
        const y = Number(cell.dataset.y);
        const nextValue = nextGame.board[y][x];
        const prevValue = prevGame && prevGame.board && prevGame.board[y] ? prevGame.board[y][x] : null;

        if (prevValue !== nextValue) {
          if (nextValue === 1) {
            cell.innerHTML = "<div class='stone stone-player'>●</div>";
          } else if (nextValue === 2) {
            cell.innerHTML = "<div class='stone stone-bot'>●</div>";
          } else {
            cell.innerHTML = "";
          }
        }

        if (nextGame.state === 'FINISHED') cell.classList.add('disabled');
        else cell.classList.remove('disabled');
      });
    }

    function renderStatus() {
      const statusEl = document.getElementById('gameStatus');
      if (!statusEl) return;
      if (gameData.state === 'FINISHED') {
        statusEl.classList.add('finished');
        if (gameData.result === 'P1_WIN') statusEl.textContent = gameData.gameMode === 'TWO_PLAYERS' ? '🎉 Người chơi 1 thắng!' : '🎉 Bạn thắng!';
        else if (gameData.result === 'P2_WIN') statusEl.textContent = gameData.gameMode === 'TWO_PLAYERS' ? '🎉 Người chơi 2 thắng!' : '😔 Máy thắng';
        else statusEl.textContent = '🤝 Hòa';
      } else {
        statusEl.classList.remove('finished');
        if (gameData.gameMode === 'TWO_PLAYERS') {
          statusEl.textContent = gameData.currentPlayer === 1 ? 'Lượt người chơi 1 (Xanh)' : 'Lượt người chơi 2 (Đen)';
        } else {
          statusEl.textContent = gameData.currentPlayer === 1 ? 'Lượt của bạn' : 'Máy đang suy nghĩ...';
        }
      }
    }

    function renderMoves() {
      const totalMovesEl = document.getElementById('totalMoves');
      if (totalMovesEl) totalMovesEl.textContent = gameData.moves.length;

      const historyEl = document.getElementById('moveHistory');
      if (!historyEl) return;
      historyEl.innerHTML = '';
      for (let i = 0; i < gameData.moves.length; i++) {
        const [x, y] = gameData.moves[i];
        const player = gameData.gameMode === 'TWO_PLAYERS'
                ? (i % 2 === 0 ? 'Người chơi 1' : 'Người chơi 2')
                : (i % 2 === 0 ? 'Bạn' : 'Máy');
        const histClass = i % 2 === 0 ? 'player' : 'bot';
        const col = String.fromCharCode('A'.charCodeAt(0) + x);
        const row = y + 1;
        const item = document.createElement('div');
        item.className = 'history-item ' + histClass;
        item.textContent = '#' + (i + 1) + ': ' + player + ' - ' + col + row;
        historyEl.appendChild(item);
      }
    }

    function updateButtons() {
      const undoBtn = document.getElementById('undoBtn');
      const resignBtn = document.getElementById('resignBtn');
      if (undoBtn) {
        const minMovesToUndo = gameData.gameMode === 'TWO_PLAYERS' ? 1 : 2;
        undoBtn.disabled = gameData.state === 'FINISHED' || gameData.moves.length < minMovesToUndo;
      }
      if (resignBtn) resignBtn.disabled = gameData.state === 'FINISHED';
    }

    function startNewWithOptions() {
      const mode = gameData.gameMode === 'TWO_PLAYERS' ? 'pvp' : 'bot';
      const diffEl = document.getElementById('quickDifficulty');
      const diff = diffEl ? diffEl.value : 'medium';
      const size = document.getElementById('quickSize').value;
      window.location.href = offlineBasePath + '?action=new&mode=' + mode + '&difficulty=' + diff + '&size=' + size;
    }
  </script>
</body>
</html>
