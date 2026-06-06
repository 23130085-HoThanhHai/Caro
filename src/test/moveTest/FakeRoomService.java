package vn.edu.hcmuaf.fit.demo3.service;

import vn.edu.hcmuaf.fit.demo3.model.Room;
import java.sql.SQLException;
import java.util.Optional;

// Lớp giả lập RoomService kế thừa từ lớp chính để ghi đè phương thức truy vấn phòng
public class FakeRoomService extends RoomService {
    private Room mockRoom;

    // Hàm tiện ích cho phép cấu hình dữ liệu phòng mong muốn trước khi bấm chạy Test Case
    public void setRoom(Room room) {
        this.mockRoom = room;
    }

    @Override
    public Optional<Room> getRoomByCode(String roomCode) throws SQLException {
        if (mockRoom != null && mockRoom.getRoomCode().equalsIgnoreCase(roomCode)) {
            return Optional.of(mockRoom);
        }
        return Optional.empty();
    }
}