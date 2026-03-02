package net.akat;

import org.postgresql.util.PSQLException;

import java.sql.*;
import java.util.UUID;

public class Database {
    private Connection connection;
    private final String HOST = "31.57.34.237";
    private final String PORT = "5432";
    private final String DB_NAME = "pointauc";
    private final String USERNAME = "pointauc_user";
    private final String PASSWORD = "$8jZeJyHisphWWpPQ_F6";

    private final Object lock = new Object();

    public void connect() {
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DB_NAME;
            connection = DriverManager.getConnection(url, USERNAME, PASSWORD);

            connection.setAutoCommit(true);

            Main.getInstance().getLogger().info("✅ PostgreSQL подключен успешно!");
            createTables();
        } catch (ClassNotFoundException e) {
            Main.getInstance().getLogger().severe("❌ PostgreSQL JDBC Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            Main.getInstance().getLogger().severe("❌ Ошибка подключения к PostgreSQL:");
            e.printStackTrace();
        }
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
            String url = "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DB_NAME;
            connection = DriverManager.getConnection(url, USERNAME, PASSWORD);
            connection.setAutoCommit(true);
            Main.getInstance().getLogger().info("✅ Переподключение к PostgreSQL успешно!");
        } catch (SQLException e) {
            Main.getInstance().getLogger().severe("❌ Ошибка переподключения к PostgreSQL:");
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
            } catch (PSQLException e) {
                attempt++;
                if (e.getMessage().contains("This connection has been closed") && attempt < maxRetries) {
                    Main.getInstance().getLogger().warning("Соединение разорвано, попытка " + attempt + "/" + maxRetries);
                    try {
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                e.printStackTrace();
                break;
            } catch (SQLException e) {
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
}
