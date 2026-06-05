import vn.edu.hcmuaf.fit.demo3.dao.RoomGameDao;
import vn.edu.hcmuaf.fit.demo3.dao.RoomDao;
import vn.edu.hcmuaf.fit.demo3.model.AuthUser;
import vn.edu.hcmuaf.fit.demo3.model.Room;
import vn.edu.hcmuaf.fit.demo3.model.RoomGameSnapshot;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
// -	4.14: Server trả JSON snapshot cho tiến trình Polling
public final class RoomGameService {
    private final RoomDao roomDao = new RoomDao();
    private final RoomGameDao roomGameDao = new RoomGameDao();
    private final ConcurrentHashMap<Long, Object> roomLocks = new ConcurrentHashMap<>();

    public RoomGameSnapshot getState(AuthUser user, String roomCode, int sinceMoveNo) throws SQLException, RoomException {
        RoomContext context = loadRoomContext(roomCode);
        Room room = context.room();
        List<Long> playerIds = context.playerIds();
        ensureGameExists(room, playerIds);

        RoomGameDao.GameInfo game = roomGameDao.findLatestGameByRoom(room.getId())
                .orElseThrow(() -> new RoomException("Không tìm thấy ván trong phòng"));

        int latestMoveNo = roomGameDao.countMoves(game.id());
        boolean needFullSync = sinceMoveNo < 0 || sinceMoveNo > latestMoveNo;
        if (needFullSync) {
            List<RoomGameDao.Move> moves = roomGameDao.findMoves(game.id());
            RoomGameSnapshot snapshot = buildSnapshot(room, playerIds, game, moves, user.getId());
            snapshot.setFullSync(true);
            return snapshot;
        }

        List<RoomGameDao.Move> deltaMoves = roomGameDao.findMovesAfter(game.id(), sinceMoveNo);
        RoomGameSnapshot snapshot = baseSnapshot(room, playerIds, game, latestMoveNo, user.getId());
        snapshot.setFullSync(false);
        snapshot.setMovesDelta(deltaMoves.stream()
                .map(m -> new int[]{m.x(), m.y(), m.playerNo(), m.moveNo()})
                .toList());
        if ("P1_WIN".equalsIgnoreCase(game.result()) || "P2_WIN".equalsIgnoreCase(game.result())) {
            // Finished game needs winning line for highlighting; compute once from full moves.
            List<RoomGameDao.Move> allMoves = roomGameDao.findMoves(game.id());
            int[][] board = buildBoard(room.getBoardSize(), allMoves);
            int winnerNo = "P1_WIN".equalsIgnoreCase(game.result()) ? 1 : 2;
            snapshot.setWinningCells(findWinningLine(board, winnerNo));
        }
        return snapshot;
    }