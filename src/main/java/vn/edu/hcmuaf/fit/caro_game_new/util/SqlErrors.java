package vn.edu.hcmuaf.fit.demo3.util;

import java.sql.SQLException;

public final class SqlErrors {
    private SqlErrors() {
    }

    public static String toUserMessage(SQLException e) {
        if (e == null) {
            return "Lỗi cơ sở dữ liệu. Vui lòng thử lại.";
        }

        SQLException root = root(e);
        String msg = safeLower(root.getMessage());
        String state = root.getSQLState();
        int code = root.getErrorCode();

        // Connection / auth errors
        if (code == 1045 || msg.contains("access denied")) {
            return "Không thể kết nối CSDL (sai tài khoản/mật khẩu). Hãy kiểm tra db.user/db.password trong db.properties.";
        }

        if ((state != null && state.startsWith("08"))
                || msg.contains("communications link failure")
                || msg.contains("connection refused")
                || msg.contains("could not create connection")
                || msg.contains("the driver has not received any packets")) {
            return "Không kết nối được MySQL. Kiểm tra MySQL đang chạy và db.url trong db.properties.";
        }

        // Missing database/table
        if (code == 1049 || msg.contains("unknown database")) {
            return "Chưa có database 'caro'. Hãy import file caro.sql để tạo database + bảng.";
        }

        if (code == 1146 || (msg.contains("doesn't exist") && msg.contains("table"))) {
            return "Chưa tạo bảng trong database. Hãy chạy caro.sql trước rồi thử lại.";
        }

        // Unique constraint
        if (code == 1062 || msg.contains("duplicate entry")) {
            return "Username hoặc Email đã tồn tại.";
        }

        return "Lỗi cơ sở dữ liệu. Hãy kiểm tra cấu hình DB và log Tomcat để xem chi tiết.";
    }

    private static SQLException root(SQLException e) {
        SQLException cur = e;
        while (cur.getNextException() != null) {
            cur = cur.getNextException();
        }
        return cur;
    }

    private static String safeLower(String s) {
        if (s == null) return "";
        return s.toLowerCase();
    }
}
