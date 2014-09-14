/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.rc.db;

import java.sql.Connection;

/**
 *
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public class JDBCConfiguration {

  private String username = "SA";
  private String password;
  private String driverClass = "org.hsqldb.jdbcDriver";
  private String url = "jdbc:hsqldb:hsql://localhost:9001/apm";
  private Connection connection;
  private int connectionGet = 0;

  public void setUsername(final String username) { this.username = username; }
  public String getUsername() { return this.username; }

  public void setPassword(final String password) { this.password = password; }
  public String getPassword() { return this.password; }

  public void setDriverClass(final String driverClass) { this.driverClass = driverClass; }
  public String getDriverClass() { return this.driverClass; }

  public void setUrl(final String url) { this.url = url; }
  public String getUrl() { return this.url; }

  public void setUrlMySQL(final String serverName, final Integer portNumber) {
    if (portNumber != null) {
      this.url = String.format("jdbc:mysql://%s:%s/", serverName, portNumber);
    } else {
      this.url = String.format("jdbc:mysql://%s/", serverName);
    }
  }

  public void setUrlDerbyStandAlone(final String fileName) {
    this.url = String.format("jdbc:derby:%s;create=true", fileName);
  }

  /*
  public synchronized final Connection getConnection() throws SQLException {
    connectionGet++;
    if (connection == null) {
      Properties connectionProps = new Properties();
      connectionProps.put("user", this.username);
      if (this.password != null) { connectionProps.put("password", this.password); }

      if (driverClass != null) {
        try {
          Driver myDriver = (Driver) Class.forName(driverClass).newInstance();
          DriverManager.registerDriver(myDriver);
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        } catch (InstantiationException ex) {
          throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
          throw new RuntimeException(ex);
        }
      }
      this.connection = DriverManager.getConnection(url, connectionProps);
      System.out.println("Connected to database");
    }
    return connection;
  }

  public synchronized final void closeConnection() throws SQLException {
    connectionGet--;
    if (connectionGet <= 0 && connection != null) {
      connection.close();
      connection = null;
      connectionGet = 0;
    }
  }
  */
}
