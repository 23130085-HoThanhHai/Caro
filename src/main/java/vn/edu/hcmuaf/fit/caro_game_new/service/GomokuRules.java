package vn.edu.hcmuaf.fit.demo3.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Tiện ích (Utility Class) quản lý các quy tắc cốt lõi của trò chơi Gomoku (Caro).
 * Cung cấp các thuật toán tĩnh (static) để kiểm tra trạng thái thắng/thua độc lập với môi trường Online/Offline.
 * * [PHẠM VI NGHIỆP VỤ - UC-05: THỰC HIỆN BƯỚC ĐI]
 * Tất cả các hàm quét chuỗi và tính toán ô thắng trong lớp này phục vụ trực tiếp cho việc xác định
 * kết quả ngay sau khi người chơi gửi tọa độ nước đi ở Use Case UC-05.
 */
public final class GomokuRules {
    
    // Ngăn chặn việc khởi tạo đối tượng vì đây là lớp tiện ích thuần static
    private GomokuRules() {}

    /**
     * [UC-05: THỰC HIỆN BƯỚC ĐI] - KIỂM TRA ĐIỀU KIỆN CHIẾN THẮNG
     * Kiểm tra quân cờ vừa đặt tại tọa độ (x, y) có tạo thành chuỗi chiến thắng hay không.
     * Thuật toán sẽ quét loang theo 4 trục: Ngang, Dọc, Chéo xuôi (\), Chéo ngược (/).
     * * @param board    Ma trận 2 chiều mô phỏng bàn cờ hiện tại
     * @param x        Tọa độ cột của nước đi vừa đánh
     * @param y        Tọa độ hàng của nước đi vừa đánh
     * @param playerNo Mã định danh người chơi (1: Player 1, 2: Player 2)
     * @return true nếu tạo thành chuỗi từ 5 quân liên tiếp trở lên, ngược lại trả về false
     */
    public static boolean isWinning(int[][] board, int x, int y, int playerNo) {
        return // Trục Ngang: Quét sang Phải (1,0) và sang Trái (-1,0) + 1 (chính nó)
               count(board, x, y, playerNo, 1, 0) + count(board, x, y, playerNo, -1, 0) + 1 >= 5
               // Trục Dọc: Quét xuống Dưới (0,1) và lên Trên (0,-1) + 1
               || count(board, x, y, playerNo, 0, 1) + count(board, x, y, playerNo, 0, -1) + 1 >= 5
               // Trục Chéo Xuôi (\): Quét xuống-phải (1,1) và lên-trái (-1,-1) + 1
               || count(board, x, y, playerNo, 1, 1) + count(board, x, y, playerNo, -1, -1) + 1 >= 5
               // Trục Chéo Ngược (/): Quét xuống-trái (-1,1) và lên-phải (1,-1) + 1
               || count(board, x, y, playerNo, 1, -1) + count(board, x, y, playerNo, -1, 1) + 1 >= 5;
    }

    /**
     * [UC-05: THỰC HIỆN BƯỚC ĐI] - HÀM QUÉT TIẾN TRÊN MA TRẬN
     * Hàm đếm số lượng quân cờ cùng màu liên tiếp theo một hướng (vector dy, dx) nhất định từ ô vừa đánh.
     */
    private static int count(int[][] board, int x, int y, int p, int dx, int dy) {
        int n = board.length; // Kích thước cạnh bàn cờ (ví dụ: 15)
        int c = 0;            // Biến đếm số quân cờ trùng khớp
        int nx = x + dx;      // Tọa độ ô tiếp theo cần kiểm tra trên trục X
        int ny = y + dy;      // Tọa độ ô tiếp theo cần kiểm tra trên trục Y

        // Vòng lặp chạy tiến tới khi nào còn nằm trong ranh giới bàn cờ và ô đó có quân của người chơi p
        while (nx >= 0 && ny >= 0 && nx < n && ny < n && board[ny][nx] == p) {
            c++;        // Tăng biến đếm
            nx += dx;   // Tiếp tục bước đi tới theo hướng X
            ny += dy;   // Tiếp tục bước đi tới theo hướng Y
        }
        return c;
    }

    /**
     * [UC-05: THỰC HIỆN BƯỚC ĐI] - TRÍCH XUẤT ĐƯỜNG THẮNG HIGHLIGHT
     * Duyệt toàn bộ bàn cờ để tìm kiếm và trích xuất đúng chuỗi 5 tọa độ ô cờ chiến thắng.
     * Mục đích: Trả về cho giao diện Frontend vẽ đường gạch nối chúc mừng người thắng cuộc sau khi hoàn thành UC-05.
     * * @param board    Ma trận bàn cờ hiện tại
     * @param playerNo Mã định danh người chơi vừa chiến thắng
     * @return Danh sách chứa 5 phần tử mảng int[2] tương ứng {x, y} của chuỗi thắng. Trả về mảng rỗng nếu không thấy.
     */
    public static List<int[]> findWinningLine(int[][] board, int playerNo) {
        int n = board.length;
        // Định nghĩa 4 hướng vector dịch chuyển để quét toàn diện bàn cờ
        int[][] dirs = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

        // Duyệt qua từng ô cờ trên ma trận 15x15
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                // Nếu ô cờ không phải của người thắng thì bỏ qua, duyệt ô tiếp theo
                if (board[y][x] != playerNo) continue;
                
                // Thử quét theo cả 4 hướng xuất phát từ ô cờ này
                for (int[] d : dirs) {
                    List<int[]> line = collectLine(board, x, y, playerNo, d[0], d[1]);
                    // Nếu hướng quét này thu thập được chuỗi >= 5 quân, trích xuất đúng 5 quân đầu tiên và trả về
                    if (line.size() >= 5) return line.subList(0, 5);
                }
            }
        }
        return List.of(); // Trả về danh sách trống nếu trận đấu chưa kết thúc/hoặc là trận hòa
    }

    /**
     * [UC-05: THỰC HIỆN BƯỚC ĐI] - THU THẬP TỌA ĐỘ
     * Hàm bổ trợ thu thập tất cả các tọa độ ô cờ cùng màu nối tiếp nhau tính từ vị trí gốc (x, y).
     */
    private static List<int[]> collectLine(int[][] board, int x, int y, int playerNo, int dx, int dy) {
        int n = board.length;
        List<int[]> line = new ArrayList<>();
        int nx = x;
        int ny = y;

        // Di chuyển dọc theo hướng vector và thêm tọa độ vào danh sách nếu thỏa mãn điều kiện cùng màu quân
        while (nx >= 0 && ny >= 0 && nx < n && ny < n && board[ny][nx] == playerNo) {
            line.add(new int[]{nx, ny});
            nx += dx;
            ny += dy;
        }
        return line;
    }
}