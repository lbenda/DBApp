/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.rc.db;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Un-closable connection
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public class DBAppConnection implements Connection {

  private static final Logger LOG = Logger.getLogger(DBAppConnection.class.getName());

  private Connection connection;

  public DBAppConnection(Connection connection) {
    this.connection = connection;
  }

  public void realyClose() throws SQLException {
    connection.close();
  }

  @Override
  public void finalize() {
    try {
      realyClose();
    } catch (SQLException e) {
      LOG.log(Level.SEVERE, "Connection close failed", e);
    }
  }

  @Override
  public Statement createStatement() throws SQLException { return connection.createStatement(); }
  @Override
  public PreparedStatement prepareStatement(String sql) throws SQLException { return connection.prepareStatement(sql); }
  @Override
  public CallableStatement prepareCall(String sql) throws SQLException { return connection.prepareCall(sql); }
  @Override
  public String nativeSQL(String sql) throws SQLException { return connection.nativeSQL(sql); }
  @Override
  public void setAutoCommit(boolean autoCommit) throws SQLException { connection.setAutoCommit(autoCommit); }
  @Override
  public boolean getAutoCommit() throws SQLException { return connection.getAutoCommit(); }
  @Override
  public void commit() throws SQLException { connection.commit(); }
  @Override
  public void rollback() throws SQLException { connection.rollback(); }
  @Override
  public void close() throws SQLException {
    LOG.log(Level.FINE, "Close connection is called");
  }
  @Override
  public boolean isClosed() throws SQLException { return connection.isClosed(); }
  @Override
  public DatabaseMetaData getMetaData() throws SQLException { return connection.getMetaData(); }
  @Override
  public void setReadOnly(boolean readOnly) throws SQLException { connection.setReadOnly(readOnly); }
  @Override
  public boolean isReadOnly() throws SQLException { return connection.isReadOnly(); }
  @Override
  public void setCatalog(String catalog) throws SQLException { connection.setCatalog(catalog); }
  @Override
  public String getCatalog() throws SQLException { return connection.getCatalog(); }
  @Override
  public void setTransactionIsolation(int level) throws SQLException { connection.setTransactionIsolation(level); }
  @Override
  public int getTransactionIsolation() throws SQLException { return connection.getTransactionIsolation(); }
  @Override
  public SQLWarning getWarnings() throws SQLException { return connection.getWarnings(); }
  @Override
  public void clearWarnings() throws SQLException { connection.clearWarnings(); }
  @Override
  public Statement createStatement(int arg0, int arg1) throws SQLException { return connection.createStatement(arg0, arg1); }
  @Override
  public PreparedStatement prepareStatement(String arg0, int arg1, int arg2) throws SQLException { return connection.prepareStatement(arg0, arg1, arg2); }
  @Override
  public CallableStatement prepareCall(String arg0, int arg1, int arg2) throws SQLException { return connection.prepareCall(arg0, arg1, arg2); }
  @Override
  public Map<String, Class<?>> getTypeMap() throws SQLException { return connection.getTypeMap(); }
  @Override
  public void setTypeMap(Map<String, Class<?>> map) throws SQLException { connection.setTypeMap(map);  }
  @Override
  public void setHoldability(int holdability) throws SQLException { connection.setHoldability(holdability); }
  @Override
  public int getHoldability() throws SQLException { return connection.getHoldability(); }
  @Override
  public Savepoint setSavepoint() throws SQLException { return connection.setSavepoint(); }
  @Override
  public Savepoint setSavepoint(String name) throws SQLException { return connection.setSavepoint(name); }
  @Override
  public void rollback(Savepoint savepoint) throws SQLException { connection.rollback(savepoint); }
  @Override
  public void releaseSavepoint(Savepoint savepoint) throws SQLException { connection.releaseSavepoint(savepoint); }
  @Override
  public Statement createStatement(int arg0, int arg1, int arg2) throws SQLException { return connection.createStatement(arg0, arg1, arg2); }
  @Override
  public PreparedStatement prepareStatement(String arg0, int arg1, int arg2, int arg3) throws SQLException { return connection.prepareStatement(arg0, arg1, arg2, arg3); }
  @Override
  public CallableStatement prepareCall(String arg0, int arg1, int arg2, int arg3) throws SQLException { return connection.prepareCall(arg0, arg1, arg2, arg3); }
  @Override
  public PreparedStatement prepareStatement(String arg0, int arg1) throws SQLException { return connection.prepareStatement(arg0, arg1); }
  @Override
  public PreparedStatement prepareStatement(String arg0, int[] arg1) throws SQLException { return connection.prepareStatement(arg0, arg1); }
  @Override
  public PreparedStatement prepareStatement(String arg0, String[] arg1) throws SQLException { return connection.prepareStatement(arg0, arg1); }
  @Override
  public Clob createClob() throws SQLException { return connection.createClob(); }
  @Override
  public Blob createBlob() throws SQLException { return connection.createBlob(); }
  @Override
  public NClob createNClob() throws SQLException { return connection.createNClob(); }
  @Override
  public SQLXML createSQLXML() throws SQLException { return connection.createSQLXML(); }
  @Override
  public boolean isValid(int timeout) throws SQLException { return connection.isValid(timeout); }
  @Override
  public void setClientInfo(String arg0, String arg1) throws SQLClientInfoException { connection.setClientInfo(arg0, arg1); }
  @Override
  public void setClientInfo(Properties properties) throws SQLClientInfoException { connection.setClientInfo(properties); }
  @Override
  public String getClientInfo(String name) throws SQLException { return connection.getClientInfo(name); }
  @Override
  public Properties getClientInfo() throws SQLException { return connection.getClientInfo(); }
  @Override
  public Array createArrayOf(String arg0, Object[] arg1) throws SQLException { return connection.createArrayOf(arg0, arg1); }
  @Override
  public Struct createStruct(String arg0, Object[] arg1) throws SQLException { return connection.createStruct(arg0, arg1); }
  @Override
  public void setSchema(String schema) throws SQLException { connection.setSchema(schema); }
  @Override
  public String getSchema() throws SQLException { return connection.getSchema(); }
  @Override
  public void abort(Executor executor) throws SQLException { connection.abort(executor); }
  @Override
  public void setNetworkTimeout(Executor arg0, int arg1) throws SQLException { connection.setNetworkTimeout(arg0, arg1); }
  @Override
  public int getNetworkTimeout() throws SQLException { return connection.getNetworkTimeout(); }
  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException { return connection.unwrap(iface); }
  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException { return connection.isWrapperFor(iface); }
}
