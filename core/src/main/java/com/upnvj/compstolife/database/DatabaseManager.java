package com.upnvj.compstolife.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/compstolife_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public void updateScore(String username, int score) {
        new Thread(() -> {
            String sql = "UPDATE tbl_users SET score = ? WHERE username = ?";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, score);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
                System.out.println("Score updated for " + username);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
