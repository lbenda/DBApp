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
package cz.lbenda.dbapp.rc.db;

import cz.lbenda.dbapp.rc.SessionConfiguration;
import org.apache.jackrabbit.test.LogPrintWriter;

import java.io.File;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.*;
import java.util.logging.Logger;
import javax.sql.DataSource;

/** Implementation of data source
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/23/14.
 */
public class DBAppDataSource implements DataSource {

  private final static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DBAppDataSource.class);

  public interface DBAppDataSourceExceptionListener {
    void onDBAppDataSourceException(Exception e);
  }

  private final SessionConfiguration sessionConfiguration;
  private final List<DBAppDataSourceExceptionListener> listeners = new ArrayList<>();

  public DBAppDataSource(SessionConfiguration sessionConfiguration) {
    this.sessionConfiguration = sessionConfiguration;
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
    return createConnection(sessionConfiguration.getJdbcConfiguration().getUsername(),
        sessionConfiguration.getJdbcConfiguration().getPassword());
  }

  private static Map<SessionConfiguration, Driver> drivers = new WeakHashMap<>();
  private static Map<SessionConfiguration, DBAppConnection> connections = new WeakHashMap<>();

  private Driver getDriver(SessionConfiguration sc) throws SQLException {
    Driver driver = drivers.get(sc);
    if (driver != null) { return driver; }

    URL[] urls = new URL[sc.getLibrariesPaths().size()];
    int i = 0;
    for (String lib : sc.getLibrariesPaths()) {
      try {
        urls[i] = (new File(lib)).toURI().toURL();
      } catch (MalformedURLException e) {
        getLogWriter().print("Problem with create URL from file: " + lib);
        e.printStackTrace(getLogWriter());
        SQLException ex = new SQLException("Problem with create URL from file" + lib, e);
        onException(ex);
        throw ex;
      }
      i++;
    }

    try {
      URLClassLoader urlCl = new URLClassLoader(urls, System.class.getClassLoader());
      if (sc.getJdbcConfiguration().getDriverClass() == null
          || "".equals(sc.getJdbcConfiguration().getDriverClass())) {
        sc.getJdbcConfiguration().setDriverClass("org.hsqldb.jdbcDriver");
      }
      Class driverCls = urlCl.loadClass(sc.getJdbcConfiguration().getDriverClass());
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

  private Connection createConnection(String username, String password) throws SQLException {
    DBAppConnection connection = this.connections.get(sessionConfiguration);
    if (connection != null && !connection.isClosed()) { return connection; }
    Properties connectionProps = new Properties();
    connectionProps.put("user", username);
    connectionProps.put("password", password);
    Driver driver = getDriver(sessionConfiguration);
    try {
      connection = new DBAppConnection(driver.connect(sessionConfiguration.getJdbcConfiguration().getUrl(), connectionProps));
      connections.put(sessionConfiguration, connection);
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

  public void removeListener(DBAppDataSourceExceptionListener listener) {
    this.listeners.remove(listener);
  }
}
