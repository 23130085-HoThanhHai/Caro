package vn.edu.hcmuaf.fit.caro_game_new.model;

public enum RoomStatus {
    WAITING,
    PLAYING,
    CLOSED;

    public static RoomStatus fromDbValue(String dbValue) {
        if (dbValue == null) return WAITING;
        return switch (dbValue.toUpperCase()) {
            case "OPEN", "WAITING" -> WAITING;
            case "IN_GAME", "PLAYING" -> PLAYING;
            default -> CLOSED;
        };
    }

    public String toDbValue() {
        return switch (this) {
            case WAITING -> "OPEN";
            case PLAYING -> "IN_GAME";
            case CLOSED -> "CLOSED";
        };
    }
}
