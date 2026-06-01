package vn.edu.hcmuaf.fit.demo3.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DbUtil {

    private DbUtil() {
    }

    private static final String DB_URL = "jdbc:mysql://mysql-1cd57459-st-a50a.k.aivencloud.com:14004/defaultdb?useSSL=true&requireSSL=true&serverTimezone=UTC";
    private static final String DB_USER = "avnadmin";
    private static final String DB_PASSWORD = "";
    private static Exception INIT_ERROR;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("[DbUtil] ✓ Đã load MySQL Driver thành công");
        } catch (ClassNotFoundException e) {
            INIT_ERROR = new RuntimeException("Không tìm thấy MySQL JDBC Driver", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (INIT_ERROR != null) {
            throw new SQLException("Lỗi khởi tạo database driver: " + INIT_ERROR.getMessage(), INIT_ERROR);
        }

        try {
            System.out.println("[DbUtil] Kết nối: " + DB_URL);
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("[DbUtil] ✓ Kết nối Aiven Cloud thành công");
            return connection;
        } catch (SQLException e) {
            System.err.println("[DbUtil] ✗ Lỗi kết nối database:");
            System.err.println("  - Error: " + e.getMessage());
            throw e;
        }
    }
}