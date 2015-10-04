/*
 * Copyright 2014 Lukas Benda <lbenda at lbenda.cz>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.lbenda.dataman.db;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import javax.sql.DataSource;

import cz.lbenda.common.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.test.LogPrintWriter;

/** Implementation of data source
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/23/14.
 */
public class DatamanDataSource implements DataSource {

  private final static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DatamanDataSource.class);

  public interface DBAppDataSourceExceptionListener {
    void onDBAppDataSourceException(Exception e);
  }

  private final DbConfig dbConfig;
  private final List<DBAppDataSourceExceptionListener> listeners = new ArrayList<>();

  public DatamanDataSource(DbConfig dbConfig) {
    this.dbConfig = dbConfig;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return createConnection();
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return createConnection(username, password);
  }

  private PrintWriter out;

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    if (out == null) { return new LogPrintWriter(LOG); }
    return out;
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    this.out = out;
  }

  private int loginTimeout = 0;

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    this.loginTimeout = seconds;
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return loginTimeout;
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    try {
      getLogWriter().print("The getParentLogger isn't supported. SLF4J is used.");
    } catch (SQLException e) { /* never heppend */ }
    throw new SQLFeatureNotSupportedException("The getParentLogger isn't supported. SLF4J is used.");
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    getLogWriter().print("This data source didn't implement wraping and unwraping");
    throw new SQLException("This data source didn't implement wraping and unwraping");
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }

  private Connection createConnection() throws SQLException, RuntimeException {
    return createConnection(dbConfig.getJdbcConfiguration().getUsername(),
        dbConfig.getJdbcConfiguration().getPassword());
  }

  private static final Map<DbConfig, Driver> drivers = new WeakHashMap<>();
  private static final Map<DbConfig, DatamanConnection> connections = new WeakHashMap<>();
  private static final Map<DatamanConnection, Date> lastConnectionUse = new WeakHashMap<>();

  private Driver getDriver(DbConfig sc) throws SQLException {
    Driver driver = drivers.get(sc);
    if (driver != null) { return driver; }

    try {
      if (StringUtils.isBlank(sc.getJdbcConfiguration().getDriverClass())) {
        throw new IllegalStateException("The driver must point to class");
      }
      Class driverCls;
      try {
        driverCls = ClassLoaderHelper.getClassFromLibs(sc.getJdbcConfiguration().getDriverClass(),
            sc.getLibrariesPaths(), true);
      } catch (ClassNotFoundException ce) {
        try {
          driverCls = this.getClass().getClassLoader().loadClass(sc.getJdbcConfiguration().getDriverClass());
          LOG.warn("The driver wasn't found in given libraries, but one is on classpath. The driver from classpath will be used.");
        } catch (ClassNotFoundException cf) {
          throw ce;
        }
      }
      driver = (Driver) driverCls.newInstance();
      drivers.put(sc, driver);
      return driver;
    } catch (ClassNotFoundException | SecurityException
        | InstantiationException | IllegalAccessException | IllegalArgumentException e) {
      getLogWriter().print("Filed to create connection");
      e.printStackTrace(getLogWriter());
      SQLException ex = new SQLException("Filed to create connection", e);
      onException(ex);
      throw ex;
    }
  }

  private void scheduleUnconnect(DatamanConnection connection) {
    lastConnectionUse.put(connection, new Date());
    if (connection.getConnectionTimeout() > 0) {
      (new Timer()).schedule(new timerTask(), connection.getConnectionTimeout());
    }
  }

  private Connection createConnection(String username, String password) throws SQLException {
    DatamanConnection connection = connections.get(dbConfig);
    if (connection != null && !connection.isClosed()) {
      scheduleUnconnect(connection);
      return connection;
    }
    Properties connectionProps = new Properties();
    if (!StringUtils.isEmpty(username)) { connectionProps.put("user", username); }
    if (!StringUtils.isEmpty(password)) { connectionProps.put("password", password); }
    Driver driver = getDriver(dbConfig);
    try {
      connection = new DatamanConnection(driver.connect(dbConfig.getJdbcConfiguration().getUrl(), connectionProps));
      connection.setConnectionTimeout(dbConfig.getConnectionTimeout());
      connections.put(dbConfig, connection);
      scheduleUnconnect(connection);
      return connection;
    } catch (SQLException e) {
      getLogWriter().print("Filed to create connection");
      e.printStackTrace(getLogWriter());
      onException(e);
      throw e;
    }
  }

  public void onException(Exception e) {
    for (DBAppDataSourceExceptionListener l : listeners) {
      l.onDBAppDataSourceException(e);
    }
  }

  public void addListener(DBAppDataSourceExceptionListener listener) {
    this.listeners.add(listener);
  }

  @SuppressWarnings("unused")
  public void removeListener(DBAppDataSourceExceptionListener listener) {
    this.listeners.remove(listener);
  }

  /** Close all connection in pool */
  public void closeAllConnections() throws SQLException {
    DatamanConnection connection = connections.get(dbConfig);
    connection.realyClose();
  }

  private class timerTask extends TimerTask {
    @Override
    public void run() {
      Date now = new Date();
      List<DatamanConnection> remove = new ArrayList<>();
      synchronized (lastConnectionUse) {
        for (Map.Entry<DatamanConnection, Date> entry : lastConnectionUse.entrySet()) {
          try {
            if (entry.getKey().getConnectionTimeout() > 0) {
              if ((now.getTime() - entry.getValue().getTime() >= entry.getKey().getConnectionTimeout())) {
                if (!entry.getKey().isClosed()) {
                  entry.getKey().realyClose();
                }
              }
            }
            if (entry.getKey().isClosed()) {
              remove.add(entry.getKey());
            }
          } catch (SQLException e) {
            LOG.error("Failed when connection is closed", e);
          }
        }
        remove.forEach(lastConnectionUse::remove);
      }
    }
  }
}
