package Strix_decopile.database;

/**
 * Created by a.kiperku
 * Date: 14.08.2023
 */


import Strix_decopile.Utils.BasicDataSource;
import Strix_decopile.configs.MainConfig;
import Strix_decopile.logging.StrixLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager extends BasicDataSource {
    private static final String CREATE_IF_NOT_EXIST = "CREATE TABLE IF NOT EXISTS strix_platform_hwid_ban (id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY, hwid VARCHAR(32), time_expire BIGINT, reason VARCHAR(255), gm_name VARCHAR(50))";

    public static DatabaseManager getInstance() throws SQLException {
        return LazyHolder.INSTANCE;
    }

    public DatabaseManager() {
        super(MainConfig.STRIX_PLATFORM_DATABASE_DRIVER, MainConfig.STRIX_PLATFORM_DATABASE_URL, MainConfig.STRIX_PLATFORM_DATABASE_USER, MainConfig.STRIX_PLATFORM_DATABASE_PASSWORD, MainConfig.STRIX_PLATFORM_DATABASE_MAX_CONNECTIONS, MainConfig.STRIX_PLATFORM_DATABASE_MAX_CONNECTIONS, MainConfig.STRIX_PLATFORM_DATABASE_MAX_IDLE_TIMEOUT, MainConfig.STRIX_PLATFORM_DATABASE_IDLE_TEST_PERIOD, false);
        this.checkTableExist("CREATE TABLE IF NOT EXISTS strix_platform_hwid_ban (id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY, hwid VARCHAR(32), time_expire BIGINT, reason VARCHAR(255), gm_name VARCHAR(50))");
    }

    private void checkTableExist(String quietly) {
        Connection con = null;
        PreparedStatement statement = null;

        try {
            con = this.getConnection();
            statement = con.prepareStatement(quietly);
            statement.execute();
            return;
        } catch (Exception e) {
            StrixLog.error("Exception in function DatabaseManager::checkTableExist. Exception: " + e.getLocalizedMessage());
            closeQuietly(con, statement);
        } finally {
            closeQuietly(con, statement);
            StrixLog.info("Initialized database factory complete");
        }

    }

    public Connection getConnection() throws SQLException {
        return this.getConnection((Connection)null);
    }

    public static void close(Connection conn) throws SQLException {
        if (conn != null) {
            conn.close();
        }

    }

    public static void close(Statement stmt) throws SQLException {
        if (stmt != null) {
            stmt.close();
        }

    }

    public static void close(ResultSet rs) throws SQLException {
        if (rs != null) {
            rs.close();
        }

    }

    public static void closeQuietly(Connection conn) {
        try {
            close(conn);
        } catch (SQLException var2) {
        }

    }

    public static void closeQuietly(Statement stmt) {
        try {
            close(stmt);
        } catch (SQLException var2) {
        }

    }

    public static void closeQuietly(ResultSet rs) {
        try {
            close(rs);
        } catch (SQLException var2) {
        }

    }

    public static void closeQuietly(Connection conn, Statement stmt) {
        try {
            closeQuietly(stmt);
        } finally {
            closeQuietly(conn);
        }

    }

    public static void closeQuietly(Connection conn, Statement stmt, ResultSet rs) {
        try {
            closeQuietly(rs);
        } finally {
            try {
                closeQuietly(stmt);
            } finally {
                closeQuietly(conn);
            }
        }

    }

    private static class LazyHolder {
        private static final DatabaseManager INSTANCE = new DatabaseManager();

        private LazyHolder() {
        }
    }
}
