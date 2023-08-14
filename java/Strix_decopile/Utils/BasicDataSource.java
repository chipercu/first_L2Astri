package Strix_decopile.Utils;

/**
 * Created by a.kiperku
 * Date: 14.08.2023
 */


import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import javax.sql.DataSource;

import Strix_decopile.logging.StrixLog;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

public class BasicDataSource implements DataSource {
    private final PoolingDataSource _source;
    private final ObjectPool _connectionPool;

    public BasicDataSource(String driver, String connectURI, String uname, String passwd, int maxActive, int maxIdle, int idleTimeOut, int idleTestPeriod, boolean poolPreparedStatements) {
        try {
            Class.forName(driver).newInstance();
        } catch (Exception var16) {
            StrixLog.error("Unable to load JDBC driver!");
            this._source = null;
            this._connectionPool = null;
            return;
        }

        GenericObjectPool connectionPool = new GenericObjectPool((PoolableObjectFactory)null);
        connectionPool.setMaxActive(maxActive);
        connectionPool.setMaxIdle(maxIdle);
        connectionPool.setMinIdle(1);
        connectionPool.setMaxWait(-1L);
        connectionPool.setWhenExhaustedAction((byte)2);
        connectionPool.setTestOnBorrow(false);
        connectionPool.setTestWhileIdle(true);
        connectionPool.setTimeBetweenEvictionRunsMillis((long)idleTestPeriod * 1000L);
        connectionPool.setNumTestsPerEvictionRun(maxActive);
        connectionPool.setMinEvictableIdleTimeMillis((long)idleTimeOut * 1000L);
        GenericKeyedObjectPoolFactory statementPoolFactory = null;
        if (poolPreparedStatements) {
            statementPoolFactory = new GenericKeyedObjectPoolFactory((KeyedPoolableObjectFactory)null, -1, (byte)0, 0L, 1, -1);
        }

        Properties connectionProperties = new Properties();
        connectionProperties.put("user", uname);
        connectionProperties.put("password", passwd);
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectURI, connectionProperties);
        new PoolableConnectionFactory(connectionFactory, connectionPool, statementPoolFactory, "SELECT 1", false, true);
        PoolingDataSource dataSource = new PoolingDataSource(connectionPool);
        this._connectionPool = connectionPool;
        this._source = dataSource;
    }

    public Connection getConnection(Connection con) throws SQLException {
        return con != null && !con.isClosed() ? con : this._source.getConnection();
    }

    public int getBusyConnectionCount() throws SQLException {
        return this._connectionPool.getNumActive();
    }

    public int getIdleConnectionCount() throws SQLException {
        return this._connectionPool.getNumIdle();
    }

    public void shutdown() throws Exception {
        this._connectionPool.close();
    }

    public PrintWriter getLogWriter() throws SQLException {
        return this._source.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        this._source.setLogWriter(out);
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public int getLoginTimeout() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    public Connection getConnection() throws SQLException {
        return this._source.getConnection();
    }

    public Connection getConnection(String username, String password) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getLogger(BasicDataSource.class.getName());
    }
}

