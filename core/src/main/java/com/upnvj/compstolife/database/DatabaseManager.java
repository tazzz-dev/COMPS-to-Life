package com.upnvj.compstolife.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String LEGACY_DB_DIR = ".compstolife";
    private static final String DB_NAME = "compstolife.db";

    public static Connection getConnection() throws SQLException {
        try {
            Path dbPath = resolveDatabasePath();
            migrateLegacyDatabase(dbPath);
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
            configureConnection(conn);
            return conn;
        } catch (Exception e) {
            throw new SQLException("Failed to open SQLite database", e);
        }
    }

    private static Path resolveDatabasePath() throws SQLException {
        try {
            Path gameDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
            if ("assets".equalsIgnoreCase(gameDirectory.getFileName().toString()) && gameDirectory.getParent() != null) {
                gameDirectory = gameDirectory.getParent();
            }
            Files.createDirectories(gameDirectory);
            return gameDirectory.resolve(DB_NAME);
        } catch (Exception e) {
            throw new SQLException("Failed to resolve SQLite database path", e);
        }
    }

    private static void migrateLegacyDatabase(Path dbPath) throws SQLException {
        try {
            Path legacyDbPath = Paths.get(System.getProperty("user.home"), LEGACY_DB_DIR, DB_NAME);
            if (!Files.exists(dbPath) && Files.exists(legacyDbPath)) {
                Files.copy(legacyDbPath, dbPath, StandardCopyOption.COPY_ATTRIBUTES);
            }
        } catch (Exception e) {
            throw new SQLException("Failed to migrate legacy SQLite database", e);
        }
    }

    private static void configureConnection(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA busy_timeout = 5000");
            stmt.execute("PRAGMA foreign_keys = ON");
        }
    }

    public void initialize() {
        String createPlayers = """
            CREATE TABLE IF NOT EXISTS players (
                username TEXT PRIMARY KEY,
                total_score INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;
        String createSaveGames = """
            CREATE TABLE IF NOT EXISTS save_games (
                username TEXT PRIMARY KEY,
                map_name TEXT NOT NULL,
                player_tile_x REAL NOT NULL,
                player_tile_y REAL NOT NULL,
                almet_equipped INTEGER NOT NULL DEFAULT 0,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(username) REFERENCES players(username)
            )
            """;
        String createNpcProgress = """
            CREATE TABLE IF NOT EXISTS npc_progress (
                username TEXT NOT NULL,
                npc_key TEXT NOT NULL,
                last_correct INTEGER NOT NULL DEFAULT 0,
                score INTEGER NOT NULL DEFAULT 0,
                attempts INTEGER NOT NULL DEFAULT 0,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY(username, npc_key),
                FOREIGN KEY(username) REFERENCES players(username)
            )
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute(createPlayers);
            stmt.execute(createSaveGames);
            stmt.execute(createNpcProgress);
            addColumnIfMissing(conn, "save_games", "almet_equipped", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(conn, "npc_progress", "score", "INTEGER NOT NULL DEFAULT 0");
            backfillNpcScores(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void backfillNpcScores(Connection conn) throws SQLException {
        String sql = """
            UPDATE npc_progress
            SET score = CASE npc_key
                WHEN 'arya' THEN 5
                WHEN 'nadhifa' THEN 5
                WHEN 'nadia' THEN 5
                WHEN 'pakhendra' THEN 15
                WHEN 'ayu' THEN 10
                WHEN 'arka' THEN 10
                WHEN 'reyhan' THEN 10
                WHEN 'rizky' THEN 10
                WHEN 'salsa' THEN 10
                WHEN 'tasya' THEN 10
                WHEN 'zaki' THEN 10
                ELSE score
            END
            WHERE last_correct = 1 AND score = 0
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private void addColumnIfMissing(Connection conn, String tableName, String columnName, String definition) throws SQLException {
        String query = "PRAGMA table_info(" + tableName + ")";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return;
                }
            }
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    public SaveData loadOrCreatePlayer(String username) {
        ensurePlayer(username);

        String sql = """
            SELECT p.total_score, s.map_name, s.player_tile_x, s.player_tile_y, s.almet_equipped
            FROM players p
            LEFT JOIN save_games s ON s.username = p.username
            WHERE p.username = ?
            """;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String mapName = rs.getString("map_name");
                    return new SaveData(
                        rs.getInt("total_score"),
                        mapName,
                        rs.getFloat("player_tile_x"),
                        rs.getFloat("player_tile_y"),
                        rs.getInt("almet_equipped") == 1
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new SaveData(0, null, 0, 0, false);
    }

    public List<String> loadCompletedNpcQuizzes(String username) {
        List<String> npcKeys = new ArrayList<>();
        String sql = "SELECT npc_key FROM npc_progress WHERE username = ? AND last_correct = 1";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    npcKeys.add(rs.getString("npc_key"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return npcKeys;
    }

    public void saveGame(String username, int totalScore, String mapName, float playerTileX, float playerTileY, boolean almetEquipped) {
        String insertPlayer = """
            INSERT INTO players (username, total_score)
            VALUES (?, 0)
            ON CONFLICT(username) DO NOTHING
            """;
        String updatePlayer = """
            UPDATE players
            SET total_score = ?, updated_at = CURRENT_TIMESTAMP
            WHERE username = ?
            """;
        String sql = """
            INSERT INTO save_games (username, map_name, player_tile_x, player_tile_y, almet_equipped, updated_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(username) DO UPDATE SET
                map_name = excluded.map_name,
                player_tile_x = excluded.player_tile_x,
                player_tile_y = excluded.player_tile_y,
                almet_equipped = excluded.almet_equipped,
                updated_at = CURRENT_TIMESTAMP
            """;
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement insertPlayerStmt = conn.prepareStatement(insertPlayer);
                 PreparedStatement updatePlayerStmt = conn.prepareStatement(updatePlayer);
                 PreparedStatement saveStmt = conn.prepareStatement(sql)) {
                insertPlayerStmt.setString(1, username);
                insertPlayerStmt.executeUpdate();

                updatePlayerStmt.setInt(1, totalScore);
                updatePlayerStmt.setString(2, username);
                updatePlayerStmt.executeUpdate();

                saveStmt.setString(1, username);
                saveStmt.setString(2, mapName);
                saveStmt.setFloat(3, playerTileX);
                saveStmt.setFloat(4, playerTileY);
                saveStmt.setInt(5, almetEquipped ? 1 : 0);
                saveStmt.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveNpcQuizResult(String username, String npcKey, boolean correct, int score) {
        ensurePlayer(username);

        String sql = """
            INSERT INTO npc_progress (username, npc_key, last_correct, score, attempts, updated_at)
            VALUES (?, ?, ?, ?, 1, CURRENT_TIMESTAMP)
            ON CONFLICT(username, npc_key) DO UPDATE SET
                last_correct = CASE
                    WHEN npc_progress.last_correct = 1 THEN 1
                    ELSE excluded.last_correct
                END,
                score = CASE
                    WHEN npc_progress.last_correct = 1 THEN npc_progress.score
                    ELSE excluded.score
                END,
                attempts = npc_progress.attempts + 1,
                updated_at = CURRENT_TIMESTAMP
            """;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, npcKey);
            pstmt.setInt(3, correct ? 1 : 0);
            pstmt.setInt(4, correct ? score : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateScore(String username, int score) {
        ensurePlayer(username);

        String sql = """
            UPDATE players
            SET total_score = ?, updated_at = CURRENT_TIMESTAMP
            WHERE username = ?
            """;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, score);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void ensurePlayer(String username) {
        String sql = """
            INSERT INTO players (username, total_score)
            VALUES (?, 0)
            ON CONFLICT(username) DO NOTHING
            """;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class SaveData {
        public final int totalScore;
        public final String mapName;
        public final float playerTileX;
        public final float playerTileY;
        public final boolean almetEquipped;

        public SaveData(int totalScore, String mapName, float playerTileX, float playerTileY, boolean almetEquipped) {
            this.totalScore = totalScore;
            this.mapName = mapName;
            this.playerTileX = playerTileX;
            this.playerTileY = playerTileY;
            this.almetEquipped = almetEquipped;
        }

        public boolean hasPosition() {
            return mapName != null && !mapName.isBlank();
        }
    }
}
