package net.akat;

import java.io.File;
import java.sql.*;
import java.util.Locale;
import java.util.UUID;

public class Database {
    private Connection connection;

    private final Object lock = new Object();
    private DbType dbType = DbType.POSTGRESQL;

    private String host;
    private String port;
    private String dbName;
    private String username;
    private String password;
    private String sqliteFile;

    public void connect() {
        loadConfig();

        try {
            if (dbType == DbType.SQLITE) {
                Class.forName("org.sqlite.JDBC");
                File sqliteDb = new File(Main.getInstance().getDataFolder(), sqliteFile);
                File parent = sqliteDb.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                String url = "jdbc:sqlite:" + sqliteDb.getAbsolutePath();
                connection = DriverManager.getConnection(url);
                Main.getInstance().getLogger().info("✅ SQLite подключен успешно: " + sqliteDb.getAbsolutePath());
            } else {
                Class.forName("org.postgresql.Driver");
                String url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
                connection = DriverManager.getConnection(url, username, password);
                Main.getInstance().getLogger().info("✅ PostgreSQL подключен успешно!");
            }

            connection.setAutoCommit(true);
            createTables();
        } catch (ClassNotFoundException e) {
            Main.getInstance().getLogger().severe("❌ JDBC Driver not found for: " + dbType.name());
            e.printStackTrace();
        } catch (SQLException e) {
            Main.getInstance().getLogger().severe("❌ Ошибка подключения к БД (" + dbType.name() + "):");
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        var config = Main.getInstance().getConfig();
        String configuredType = config.getString("database.type", "postgresql").toLowerCase(Locale.ROOT);
        dbType = configuredType.equals("sqlite") ? DbType.SQLITE : DbType.POSTGRESQL;

        host = config.getString("database.postgresql.host", "31.57.34.237");
        port = config.getString("database.postgresql.port", "5432");
        dbName = config.getString("database.postgresql.name", "pointauc");
        username = config.getString("database.postgresql.username", "pointauc_user");
        password = config.getString("database.postgresql.password", "");

        sqliteFile = config.getString("database.sqlite.file", "test/pointauc.db");
    }

    private Connection getValidConnection() throws SQLException {
        synchronized (lock) {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                Main.getInstance().getLogger().warning("Соединение с БД разорвано, переподключаемся...");
                disconnect();
                reconnect();
            }
            return connection;
        }
    }

    private void reconnect() throws SQLException {
        try {
            if (dbType == DbType.SQLITE) {
                File sqliteDb = new File(Main.getInstance().getDataFolder(), sqliteFile);
                String url = "jdbc:sqlite:" + sqliteDb.getAbsolutePath();
                connection = DriverManager.getConnection(url);
            } else {
                String url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
                connection = DriverManager.getConnection(url, username, password);
            }
            connection.setAutoCommit(true);
            Main.getInstance().getLogger().info("✅ Переподключение к БД успешно (" + dbType.name() + ")!");
        } catch (SQLException e) {
            Main.getInstance().getLogger().severe("❌ Ошибка переподключения к БД:");
            throw e;
        }
    }

    public void createTables() {
        try (Statement stmt = getValidConnection().createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS balances (uuid TEXT PRIMARY KEY, points INTEGER)");
            stmt.execute("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT)");
            stmt.execute("INSERT INTO settings (key, value) VALUES ('auction_enabled', 'false') ON CONFLICT (key) DO NOTHING");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void save(UUID uuid, int points) {
        executeWithRetry(() -> {
            try (PreparedStatement stmt = getValidConnection().prepareStatement(
                    "INSERT INTO balances (uuid, points) VALUES (?, ?) " +
                            "ON CONFLICT (uuid) DO UPDATE SET points = EXCLUDED.points")) {
                stmt.setString(1, uuid.toString());
                stmt.setInt(2, points);
                stmt.executeUpdate();
            }
            return null;
        });
    }

    public int load(UUID uuid) {
        return executeWithRetry(() -> {
            try (PreparedStatement stmt = getValidConnection().prepareStatement("SELECT points FROM balances WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("points");
                }
            }
            return 0;
        });
    }

    private <T> T executeWithRetry(SQLCallable<T> callable) {
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                return callable.call();
            } catch (SQLException e) {
                attempt++;
                if (attempt < maxRetries) {
                    Main.getInstance().getLogger().warning("Ошибка БД, попытка " + attempt + "/" + maxRetries + ": " + e.getMessage());
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                e.printStackTrace();
                break;
            }
        }

        if (callable instanceof DefaultValue) {
            return ((DefaultValue<T>) callable).getDefaultValue();
        }
        return null;
    }

    @FunctionalInterface
    private interface SQLCallable<T> {
        T call() throws SQLException;
    }

    private interface DefaultValue<T> {
        T getDefaultValue();
    }

    public void disconnect() {
        synchronized (lock) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    Main.getInstance().getLogger().info("Соединение с БД закрыто");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isAuctionEnabled() {
        return executeWithRetry(() -> {
            try (PreparedStatement stmt = getValidConnection().prepareStatement("SELECT value FROM settings WHERE key = 'auction_enabled'")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return Boolean.parseBoolean(rs.getString("value"));
                }
            }
            return true;
        });
    }

    public void setAuctionEnabled(boolean enabled) {
        executeWithRetry(() -> {
            try (PreparedStatement stmt = getValidConnection().prepareStatement(
                    "INSERT INTO settings (key, value) VALUES ('auction_enabled', ?) " +
                            "ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value")) {
                stmt.setString(1, String.valueOf(enabled));
                stmt.executeUpdate();
            }
            return null;
        });
    }

    private enum DbType {
        POSTGRESQL,
        SQLITE
    }
}
