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
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.jackrabbit.test.LogPrintWriter;
import org.slf4j.LoggerFactory;

/**
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/23/14.
 */
public class DBAppDataSource implements DataSource {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DBAppDataSource.class);

  private final SessionConfiguration sessionConfiguration;

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

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return new LogPrintWriter(LOG);
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
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
    LOG.debug("The getParentLogger isn't supported. SLF4J is used.");
    throw new SQLFeatureNotSupportedException("The getParentLogger isn't supported. SLF4J is used.");
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    LOG.debug("This data source didn't implement wraping and unwraping");
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

  private Connection createConnection(String username, String password) throws SQLException, RuntimeException {
    URL[] urls = new URL[sessionConfiguration.getLibrariesPaths().size()];
    int i = 0;
    for (String lib : sessionConfiguration.getLibrariesPaths()) {
      LOG.trace("Path to jar: " + lib);
      try {
        urls[i] = (new File(lib)).toURI().toURL();
      } catch (MalformedURLException e) {
        LOG.error("Problem with create URL from file: " + lib, e);
      }
      i++;
    }

    Properties connectionProps = new Properties();
    connectionProps.put("user", username);
    connectionProps.put("password", password);

    try {
      URLClassLoader urlCl = new URLClassLoader(urls, System.class.getClassLoader());
      if (sessionConfiguration.getJdbcConfiguration().getDriverClass() == null
          || "".equals(sessionConfiguration.getJdbcConfiguration().getDriverClass())) {
        sessionConfiguration.getJdbcConfiguration().setDriverClass("org.hsqldb.jdbcDriver");
      }
      LOG.trace("Read driver class: " + sessionConfiguration.getJdbcConfiguration().getDriverClass());
      Class driverCls = urlCl.loadClass(sessionConfiguration.getJdbcConfiguration().getDriverClass());
      Driver driver = (Driver) driverCls.newInstance();
      return driver.connect(sessionConfiguration.getJdbcConfiguration().getUrl(), connectionProps);
    } catch (ClassNotFoundException | SecurityException
            | InstantiationException | IllegalAccessException | IllegalArgumentException e) {
      LOG.error("Filed to create connection", e);
      ((InvocationTargetException) e).printStackTrace(new LogPrintWriter(LOG));
      throw new RuntimeException("Filed to create connection", e);    }
  }
}
