package vn.edu.hcmuaf.fit.demo3.web.offline;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.demo3.model.OfflineGame;
import vn.edu.hcmuaf.fit.demo3.model.OfflineGame.Difficulty;
import vn.edu.hcmuaf.fit.demo3.model.OfflineGame.GameMode;
import vn.edu.hcmuaf.fit.demo3.service.OfflineGameService;

import java.io.IOException;

@WebServlet(name = "offlinePlayServlet", urlPatterns = {"/offline-play", "/offline-bot", "/offline-pvp"})
public class OfflinePlayServlet extends HttpServlet {
    private static final String SESSION_BOT_GAME_ID = "offlineBotGameId";
    private static final String SESSION_PVP_GAME_ID = "offlinePvpGameId";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        String action = request.getParameter("action");
        String servletPath = request.getServletPath();
        GameMode routeMode = resolveRouteMode(servletPath);

        if ("new".equals(action)) {
            int boardSize = 15;
            String sizeParam = request.getParameter("size");
            if (sizeParam != null) {
                try {
                    boardSize = Integer.parseInt(sizeParam);
                    if (boardSize < 5 || boardSize > 50) boardSize = 15;
                } catch (NumberFormatException ignored) {}
            }

            String diffParam = request.getParameter("difficulty");
            Difficulty difficulty = Difficulty.MEDIUM;
            try {
                difficulty = Difficulty.valueOf(diffParam != null ? diffParam.toUpperCase() : "MEDIUM");
            } catch (IllegalArgumentException ignored) {}

            String modeParam = request.getParameter("mode");
            GameMode gameMode = resolveRequestedMode(modeParam, routeMode);
            if (gameMode == GameMode.TWO_PLAYERS) {
                difficulty = Difficulty.MEDIUM;
            }

            OfflineGame game = OfflineGameService.createGame(boardSize, difficulty, gameMode);
            session.setAttribute(sessionKeyFor(gameMode), game.getGameId());
            response.sendRedirect(request.getContextPath() + servletPath + "?action=board&gameId=" + game.getGameId());
            return;
        }

        String gameId = request.getParameter("gameId");
        if (gameId == null || gameId.isBlank()) {
            GameMode selectedMode = resolveRequestedMode(request.getParameter("mode"), routeMode);
            gameId = (String) session.getAttribute(sessionKeyFor(selectedMode));
        }

        if (gameId != null && !gameId.isBlank()) {
            OfflineGame game = OfflineGameService.getGame(gameId);
            if (game != null) {
                if (routeMode != null && game.getGameMode() != routeMode) {
                    game = null;
                }
            }
            if (game != null) {
                session.setAttribute(sessionKeyFor(game.getGameMode()), game.getGameId());
                request.setAttribute("game", game);
                request.setAttribute("offlineBasePath", request.getContextPath() + servletPath);
                request.getRequestDispatcher("/WEB-INF/jsp/offline/play.jsp").forward(request, response);
                return;
            }
        }

        request.setAttribute("offlineBasePath", request.getContextPath() + servletPath);
        request.getRequestDispatcher("/WEB-INF/jsp/offline/menu.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String action = request.getParameter("action");
        String gameId = request.getParameter("gameId");

        OfflineGame game = gameId != null && !gameId.isBlank() ? OfflineGameService.getGame(gameId) : null;
        if (game == null) {
            response.getWriter().write("{\"success\":false,\"error\":\"Game not found\"}");
            return;
        }

        if ("move".equals(action)) {
            // [UC-05.1.4]: Hệ thống nhận yêu cầu xử lý nước đi (Action: move) từ Client. [cite: 3]
            // ... (xử lý gọi playerMove hoặc localMove) ...
            try {
                int x = Integer.parseInt(request.getParameter("x"));
                int y = Integer.parseInt(request.getParameter("y"));

                boolean moved;
                if (game.getGameMode() == GameMode.TWO_PLAYERS) {
                    moved = OfflineGameService.localMove(game, x, y);
                } else {
                    moved = OfflineGameService.playerMove(game, x, y);
                }

                if (!moved) {
                    response.getWriter().write("{\"success\":false,\"error\":\"Invalid move\"}");
                    return;
                }

                if (game.getGameMode() == GameMode.VS_BOT && game.getState().toString().equals("IN_PROGRESS")) {
                    // [UC-05.2.1]: Hệ thống tự động gọi hàm botMove trong OfflineGameService sau nước đi của người chơi. [cite: 3]
                    OfflineGameService.botMove(game);
                }
                response.getWriter().write(buildGameResponse(game, true));
            } catch (Exception e) {
                response.getWriter().write("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}");
            }
        } else if ("undo".equals(action)) {
            if (OfflineGameService.undoLastRound(game)) {
                response.getWriter().write(buildGameResponse(game, true));
            } else {
                response.getWriter().write("{\"success\":false,\"error\":\"Không thể hoàn tác\"}");
            }
        } else if ("resign".equals(action)) {
            if (OfflineGameService.resign(game)) {
                response.getWriter().write(buildGameResponse(game, true));
            } else {
                response.getWriter().write("{\"success\":false,\"error\":\"Cannot resign\"}");
            }
        } else {
            response.getWriter().write("{\"success\":false,\"error\":\"Unknown action\"}");
        }
    }

    private String buildGameResponse(OfflineGame game, boolean success) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\":").append(success);
        sb.append(",\"game\":{");
        sb.append("\"gameId\":\"").append(game.getGameId()).append("\",");
        sb.append("\"boardSize\":").append(game.getBoardSize()).append(",");
        sb.append("\"difficulty\":\"").append(game.getDifficulty()).append("\",");
        sb.append("\"gameMode\":\"").append(game.getGameMode()).append("\",");
        sb.append("\"state\":\"").append(game.getState()).append("\",");
        sb.append("\"result\":\"").append(game.getResult()).append("\",");
        sb.append("\"currentPlayer\":").append(game.getCurrentPlayer()).append(",");
        sb.append("\"board\":").append(buildBoardJson(game)).append(",");
        sb.append("\"moves\":").append(buildMovesJson(game));
        sb.append("}}");
        return sb.toString();
    }

    private String buildBoardJson(OfflineGame game) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int y = 0; y < game.getBoardSize(); y++) {
            if (y > 0) sb.append(",");
            sb.append("[");
            for (int x = 0; x < game.getBoardSize(); x++) {
                if (x > 0) sb.append(",");
                sb.append(game.getCell(x, y));
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    private String buildMovesJson(OfflineGame game) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < game.getMoves().size(); i++) {
            if (i > 0) sb.append(",");
            int[] move = game.getMoves().get(i);
            sb.append("[").append(move[0]).append(",").append(move[1]).append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    private GameMode resolveRouteMode(String servletPath) {
        if ("/offline-pvp".equalsIgnoreCase(servletPath)) return GameMode.TWO_PLAYERS;
        if ("/offline-bot".equalsIgnoreCase(servletPath)) return GameMode.VS_BOT;
        return null;
    }

    private GameMode resolveRequestedMode(String modeParam, GameMode routeMode) {
        if (routeMode != null) return routeMode;
        return "pvp".equalsIgnoreCase(modeParam) || "two_players".equalsIgnoreCase(modeParam)
                ? GameMode.TWO_PLAYERS
                : GameMode.VS_BOT;
    }

    private String sessionKeyFor(GameMode mode) {
        return mode == GameMode.TWO_PLAYERS ? SESSION_PVP_GAME_ID : SESSION_BOT_GAME_ID;
    }
}
