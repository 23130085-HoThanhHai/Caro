<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="vn.edu.hcmuaf.fit.demo3.model.Room" %>
<%@ page import="vn.edu.hcmuaf.fit.demo3.model.RoomPlayer" %>
<%
  request.setAttribute("activeNav", "");
  Room room = (Room) request.getAttribute("room");
  Long currentUserId = (Long) request.getAttribute("currentUserId");
%>
<!doctype html>
<html lang="vi">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Phòng <%= room != null ? room.getRoomCode() : "" %> | Gomoku Zen</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/app.css" />
  <style>
    .room-layout { display:grid; grid-template-columns: 1fr 320px; gap:18px; }
    .board { display:grid; gap:0; background:#fff; border:1px solid #e5e7eb; border-radius:10px; padding:8px; grid-template-columns: repeat(var(--board-size), 1fr); }
    .cell { width:30px; height:30px; border:1px solid rgba(16,24,40,0.1); background:#fff; cursor:pointer; display:flex; align-items:center; justify-content:center; font-size:20px; }
    .cell.win-cell { background: rgba(241,196,15,0.25); }
    .stone {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      animation: stonePlace 0.25s ease-out;
      box-shadow: 0 2px 6px rgba(0,0,0,0.15);
    }
    .stone-player {
      background: linear-gradient(135deg, #1abc9c 0%, #16a085 100%);
      color: white;
    }
    .stone-bot {
      background: linear-gradient(135deg, #2c3e50 0%, #1a252f 100%);
      color: white;
    }
    @keyframes stonePlace {
      0% { transform: scale(0.3); opacity: 0; }
      100% { transform: scale(1); opacity: 1; }
    }
    .btn-replay {
      display: none;
      margin-top: 8px;
      width: 100%;
      padding: 9px 12px;
      border: none;
      border-radius: 10px;
      font-weight: 700;
      cursor: pointer;
      background: var(--accent);
      color: #fff;
    }
    .meta { color:#667085; font-size:13px; }
    @media (max-width: 980px) { .room-layout { grid-template-columns: 1fr; } }
  </style>
</head>
<body>
<div class="page">
  <%@ include file="/WEB-INF/jsp/common/header.jspf" %>
  <main class="main" role="main">
    <section class="section">
      <div class="container" style="max-width: 900px;">
        <% if (room == null) { %>
        <div class="card" style="padding:20px;">Không tìm thấy phòng.</div>
        <% } else { %>
        <div class="card" style="padding:20px; margin-bottom:14px;">
          <h2 class="section-title" style="margin-bottom:6px;"><%= room.getName() %></h2>
          <div class="section-sub">Mã phòng: <strong><%= room.getRoomCode() %></strong></div>
        </div>

        <div class="room-layout">
          <div class="card" style="padding:12px;">
            <div id="board" class="board" style="--board-size:<%= room.getBoardSize() %>;">
              <% for (int y = 0; y < room.getBoardSize(); y++) {
                   for (int x = 0; x < room.getBoardSize(); x++) { %>
                <div class="cell" data-x="<%= x %>" data-y="<%= y %>"></div>
              <%   }
                 } %>
            </div>
          </div>

          <div style="display:grid; gap:14px;">
            <div class="card" style="padding:20px; display:grid; gap:8px;">
                <%-- Làm nổi bật và thêm nút "Copy" Mã Phòng (room.jsp) Phạm Quốc Đăng --%>
                <div style="background: #fffbeb; border: 1px dashed #f59e0b; padding: 12px; border-radius: 8px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                    <div>
                        <div style="font-size: 13px; color: #92400e; margin-bottom: 4px;">Mã phòng của bạn:</div>
                        <strong id="roomCodeText" style="font-size: 24px; color: #b45309; letter-spacing: 2px;"><%= room.getRoomCode() %></strong>
                    </div>
                    <button onclick="copyRoomCode()" class="btn btn-primary" type="button" style="padding: 8px 12px; font-size: 14px; display: flex; align-items: center; gap: 6px;">
                        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                        </svg>
                        Copy
                    </button>
                </div>

                <script>
                    function copyRoomCode() {
                        const code = document.getElementById('roomCodeText').innerText;
                        navigator.clipboard.writeText(code).then(() => {
                            alert('Đã sao chép mã phòng: ' + code);
                        }).catch(err => {
                            console.error('Không thể sao chép', err);
                        });
                    }
                </script>
              <div><strong>Trạng thái:</strong> <span id="statusText"><%= room.getStatus() %></span></div>
              <div><strong>Kích thước:</strong> <%= room.getBoardSize() %>x<%= room.getBoardSize() %></div>
              <div><strong>Số người:</strong> <span id="playersJoined"><%= room.getPlayers().size() %></span>/2</div>
              <div><strong>Lượt:</strong> <span id="turnText">Đang tải...</span></div>
              <div id="gameMessage" class="meta"></div>
              <button id="replayBtn" class="btn-replay" type="button" onclick="restartGame()">Chơi lại</button>
            </div>

            <div class="card" style="padding:20px;">
              <h3 style="margin-top:0;">Người chơi trong phòng</h3>
              <div style="display:grid; gap:8px;">
                <% for (RoomPlayer p : room.getPlayers()) { %>
                  <div style="padding:10px; border:1px solid #eee; border-radius:8px;">
                    <strong><%= p.shownName() %></strong>
                    <% if (currentUserId != null && p.userId() == currentUserId) { %>
                      <span style="color:#b45309;">(Bạn)</span>
                    <% } %>
                    - Vai trò: <%= p.role() %>
                  </div>
                <% } %>
              </div>
              <p style="margin-top:8px; color:#667085; font-size:13px;">Người chơi khác nhập mã phòng để vào ngay.</p>
            </div>


              <div class="card" style="padding:20px;">
                  <h3 style="margin-top:0;">Chat phòng</h3>

                  <div id="chatMessages"
                       style="height:250px;
                overflow-y:auto;
                border:1px solid #eee;
                padding:10px;
                border-radius:8px;
                margin-bottom:10px;">

                      <c:forEach items="${messages}" var="msg">
                          <div style="margin-bottom:8px;">
                              <strong>${msg.senderUsername}</strong>:
                                  ${msg.content}
                          </div>
                      </c:forEach>

                  </div>

                  <form id="chatForm">
                      <input type="hidden"
                             id="roomId"
                             value="<%= room.getId() %>">

                      <input type="text"
                             id="messageText"
                             placeholder="Nhập tin nhắn..."
                             maxlength="500"
                             style="width:100%;
                      padding:10px;
                      border:1px solid #ddd;
                      border-radius:8px;" />

                      <button class="btn btn-primary"
                              type="submit"
                              style="margin-top:10px;width:100%;">
                          Gửi
                      </button>
                  </form>
              </div>


            <a class="btn btn-ghost" href="<%= request.getContextPath() %>/index.jsp" style="text-align:center;">Về sảnh</a>
          </div>
        </div>
        <% } %>
      </div>
    </section>
  </main>
  <%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
</div>
<script>
  const roomCode = '<%= room != null ? room.getRoomCode() : "" %>';
  const contextPath = '<%= request.getContextPath() %>';
  let latestState = null;
  let renderedBoard = null;
  let isSending = false;

  document.addEventListener('DOMContentLoaded', () => {
    bindBoard();
    refreshState();
    setInterval(refreshState, 2000);
  });

  function bindBoard() {
    const board = document.getElementById('board');
    if (!board) return;
    board.addEventListener('click', (e) => {
      const cell = e.target.closest('.cell');
      if (!cell || isSending) return;
      const x = Number(cell.dataset.x);
      const y = Number(cell.dataset.y);
      sendMove(x, y);
    });
  }

  async function refreshState() {
    const res = await fetch(contextPath + '/room-play?code=' + encodeURIComponent(roomCode));
    const data = await res.json();
    if (!data.success) return;
    latestState = data.state;
    renderState();
  }

  async function sendMove(x, y) {
    if (!latestState) return;
    if (latestState.playersJoined < 2) return;
    if (latestState.gameStatus === 'FINISHED') return;
    if (latestState.yourPlayerNo !== latestState.currentPlayerNo) return;

    isSending = true;
    try {
      const body = 'action=move&code=' + encodeURIComponent(roomCode) + '&x=' + x + '&y=' + y;
      const res = await fetch(contextPath + '/room-play', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body
      });
      const data = await res.json();
      if (!data.success) {
        alert(data.error || 'Không thể đánh nước này');
        return;
      }
      latestState = data.state;
      renderState();
    } finally {
      isSending = false;
    }
  }

  function renderState() {
    if (!latestState) return;
    const statusText = document.getElementById('statusText');
    const playersJoined = document.getElementById('playersJoined');
    const turnText = document.getElementById('turnText');
    const msg = document.getElementById('gameMessage');

    if (statusText) statusText.textContent = latestState.gameStatus;
    if (playersJoined) playersJoined.textContent = latestState.playersJoined;
    if (msg) msg.textContent = latestState.message || '';

    if (turnText) {
      if (latestState.playersJoined < 2) turnText.textContent = 'Chờ người chơi 2';
      else if (latestState.gameStatus === 'FINISHED') {
        turnText.textContent = 'Ván đã kết thúc';
      } else {
        turnText.textContent = latestState.currentPlayerNo === 1 ? 'Người chơi 1' : 'Người chơi 2';
      }
    }

    const boardCells = document.querySelectorAll('#board .cell');
    const replayBtn = document.getElementById('replayBtn');
    const nextBoard = latestState.board;
    boardCells.forEach(cell => {
      const x = Number(cell.dataset.x);
      const y = Number(cell.dataset.y);
      const value = nextBoard[y][x];
      const prev = renderedBoard ? renderedBoard[y][x] : null;
      if (prev !== value) {
        if (value === 1) cell.innerHTML = "<div class='stone stone-player'>●</div>";
        else if (value === 2) cell.innerHTML = "<div class='stone stone-bot'>●</div>";
        else cell.innerHTML = "";
      }
      cell.classList.remove('win-cell');
    });
    renderedBoard = nextBoard.map(row => row.slice());

    if (Array.isArray(latestState.winningCells)) {
      latestState.winningCells.forEach(([x, y]) => {
        const target = document.querySelector('#board .cell[data-x="' + x + '"][data-y="' + y + '"]');
        if (target) target.classList.add('win-cell');
      });
    }

    if (latestState.gameStatus === 'FINISHED') {
      if (replayBtn) replayBtn.style.display = 'block';
    } else {
      if (replayBtn) replayBtn.style.display = 'none';
    }
  }

  async function restartGame() {
    if (!latestState || latestState.playersJoined < 2) return;
    if (!confirm('Bắt đầu ván mới với 2 người hiện tại?')) return;
    const body = 'action=restart&code=' + encodeURIComponent(roomCode);
    const res = await fetch(contextPath + '/room-play', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body
    });
    const data = await res.json();
    if (!data.success) {
      alert(data.error || 'Không thể chơi lại');
      return;
    }
    latestState = data.state;
    renderState();
  }


  document.getElementById("chatForm")
      .addEventListener("submit", async function(e){

          e.preventDefault();

          const text =
              document.getElementById("messageText").value.trim();

          if(!text) return;

          const roomId =
              document.getElementById("roomId").value;

          const body =
              "roomId=" + roomId +
              "&message=" + encodeURIComponent(text);

          const response = await fetch(
              contextPath + "/chat/send",
              {
                  method:"POST",
                  headers:{
                      "Content-Type":
                          "application/x-www-form-urlencoded"
                  },
                  body:body
              }
          );

          const result = await response.json();

          if(result.success){
              document.getElementById("messageText").value="";
              loadMessages();
          }else{
              alert(result.error);
          }
      });

  async function loadMessages(){

      const roomId =
          document.getElementById("roomId").value;

      const response = await fetch(
          contextPath + "/chat/messages?roomId=" + roomId
      );

      const data = await response.json();

      let html = "";

      data.messages.forEach(msg => {

          html += `
            <div style="margin-bottom:8px;">
                <strong>${msg.senderUsername}</strong>:
                ${msg.content}
            </div>
        `;
      });

      const box =
          document.getElementById("chatMessages");

      box.innerHTML = html;
      box.scrollTop = box.scrollHeight;
  }

  setInterval(loadMessages, 2000);

</script>
</body>
</html>
