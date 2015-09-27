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

import cz.lbenda.dataman.User;
import cz.lbenda.dataman.db.audit.Auditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/** Prepared statement which can write audit information to auditor
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 4.9.15.
 */
public class AuditPreparedStatement implements java.sql.PreparedStatement {

  private static final Logger LOG = LoggerFactory.getLogger(AuditPreparedStatement.class);

  private static ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
    public DateFormat initialValue() {
      return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    }
  };

  /**
   * All paramters for one row which is add by batch
   */
  private List<Map<Integer, String>> batchParameters = new ArrayList<>();
  /**
   * All stored paramters
   */
  private Map<Integer, String> parameterValues = new HashMap<>();

  /**
   * SQL template which is shown
   */
  private List<String> sqlTemplate = new ArrayList<>();

  /**
   * Wrapped statement on which is call delegated
   */
  private PreparedStatement wrappedStatement;

  /**
   * Object which is used for write audit information
   */
  private Auditor auditor;
  /**
   * User which work with prepared statement
   */
  private User user;

  @SuppressWarnings("unused")
  public AuditPreparedStatement(User user, Auditor auditor, Connection connection, String sql) throws SQLException {
    this(user, auditor, connection.prepareStatement(sql), sql);
  }

  private AuditPreparedStatement(User user, Auditor auditor, PreparedStatement wrappedStatement, String sql) {
    this.wrappedStatement = wrappedStatement;
    this.user = user;
    sqlTemplate.clear();
    sqlTemplate.add(sql);
    parameterValues.clear();
    batchParameters.clear();
    this.auditor = auditor;
  }

  /** Use this method instead of {@see Connection#prepareCall(String)} */
  public static AuditPreparedStatement prepareCall(User user, Auditor auditor, Connection connection, String sql) throws SQLException {
    PreparedStatement wraped = connection.prepareCall(sql);
    return new AuditPreparedStatement(user, auditor, wraped, sql);
  }

  /** Use this method instead of {@see Connection#prepareStatement(String, int)} */
  public static AuditPreparedStatement prepareStatement(User user, Auditor auditor, Connection connection,
                                                              String sql, int autoGeneratedKeys) throws SQLException {
    PreparedStatement wraped = connection.prepareStatement(sql, autoGeneratedKeys);
    return new AuditPreparedStatement(user, auditor, wraped, sql);
  }

  private void writeAudit(int[] affectedRows) {
    StringBuilder sb = new StringBuilder();
    if (sqlTemplate.size() > 1) {
      int i = 0;
      for (String sql : sqlTemplate) {
        sb.append(sql).append("\n");
        sb.append("Affected rows: ").append(affectedRows[i]).append("\n");
        i++;
      }
    }
    else if (sqlTemplate.size() == 1) {
      sb.append(sqlTemplate.get(0)).append("\n");
      if (!this.batchParameters.contains(this.parameterValues)) { this.batchParameters.add(this.parameterValues); }
      int j = 0;
      for (Map<Integer, String> parameters : this.batchParameters) {
        if (parameters != null && !parameters.isEmpty()) {
          sb.append("[");

          // For every index will be generate one field even if aren't in map
          List<Integer> list = new ArrayList<>();
          list.addAll(parameters.keySet());
          Collections.sort(list);
          int max = list.get(list.size() - 1);
          for (int i = 1; i <= max; i++) { // JDBC start from 1
            if (i > 1) { sb.append(", "); }
            sb.append(parameters.get(i));
          }

          sb.append("]\n");
          sb.append("Affected rows: ").append(affectedRows[j]).append("\n");
          j++;
        }
      }
      if (j == 0) { sb.append("Affected rows: ").append(affectedRows[j]).append("\n"); } // No affected row was writen
    } else { LOG.error("No SQL was set, nothing for logging"); }
    auditor.writePlainTextAudit(user.getUsername(), sb.toString());
  }

  @Override public ResultSet executeQuery() throws SQLException {
    return wrappedStatement.executeQuery();
  }
  @Override public int executeUpdate() throws SQLException {
    return wrappedStatement.executeUpdate();
  }
  @Override public int[] executeBatch() throws SQLException {
    int[] result = wrappedStatement.executeBatch();
    this.writeAudit(result);
    return result;
  }
  @Override public int executeUpdate(String sql, String[] columnNames) throws SQLException {
    int result = wrappedStatement.executeUpdate(sql, columnNames);
    this.sqlTemplate.add(sql);
    this.writeAudit(new int[] { result });
    return result;
  }
  @Override public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
    boolean result = wrappedStatement.execute(sql, autoGeneratedKeys);
    this.sqlTemplate.add(sql);
    if (result) { this.writeAudit(new int[] { 1 }); }
    else { this.writeAudit(new int[] { wrappedStatement.getUpdateCount() }); }
    return result;
  }
  @Override public boolean execute(String sql, int[] columnIndexes) throws SQLException {
    boolean result = wrappedStatement.execute(sql, columnIndexes);
    this.sqlTemplate.add(sql);
    if (result) { this.writeAudit(new int[] { 1 }); }
    else { this.writeAudit(new int[] { wrappedStatement.getUpdateCount() }); }
    return result;
  }
  @Override public boolean execute(String sql, String[] columnNames) throws SQLException {
    boolean result = wrappedStatement.execute(sql, columnNames);
    this.sqlTemplate.add(sql);
    if (result) { this.writeAudit(new int[] { 1 }); }
    else { this.writeAudit(new int[] { wrappedStatement.getUpdateCount() }); }
    return result;
  }
  @Override
  public ResultSet executeQuery(String sql) throws SQLException {
    return this.wrappedStatement.executeQuery(sql);
  }
  @Override
  public boolean execute() throws SQLException {
    boolean result = wrappedStatement.execute();
    if (result) { this.writeAudit(new int[] { 1 }); }
    else { this.writeAudit(new int[] { wrappedStatement.getUpdateCount() }); }
    return result;
  }
  @Override
  public int executeUpdate(String sql) throws SQLException {
    int result = wrappedStatement.executeUpdate(sql);
    this.sqlTemplate.add(sql);
    this.writeAudit(new int[] { result });
    return result;
  }
  @Override
  public boolean execute(String sql) throws SQLException {
    boolean result = wrappedStatement.execute(sql);
    this.sqlTemplate.add(sql);
    if (result) { this.writeAudit(new int[] { 1 }); }
    else { this.writeAudit(new int[] { wrappedStatement.getUpdateCount() }); }
    return result;
  }
  @Override
  public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
    int result = wrappedStatement.executeUpdate(sql, autoGeneratedKeys);
    this.sqlTemplate.add(sql);
    this.writeAudit(new int[] { result });
    return result;
  }
  @Override
  public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
    int result = wrappedStatement.executeUpdate(sql, columnIndexes);
    this.sqlTemplate.add(sql);
    this.writeAudit(new int[] { result });
    return result;
  }


  @Override
  public void setNull(int parameterIndex, int sqlType) throws SQLException {
    this.parameterValues.put(parameterIndex, null);
    this.wrappedStatement.setNull(parameterIndex, sqlType);
  }
  @Override
  public void setBoolean(int parameterIndex, boolean x) throws SQLException {
    this.parameterValues.put(parameterIndex, Boolean.toString(x));
    this.wrappedStatement.setBoolean(parameterIndex, x);
  }
  @Override
  public void setByte(int parameterIndex, byte x) throws SQLException {
    this.parameterValues.put(parameterIndex, Byte.toString(x));
    this.wrappedStatement.setByte(parameterIndex, x);
  }
  @Override
  public void setShort(int parameterIndex, short x) throws SQLException {
    this.parameterValues.put(parameterIndex, Short.toString(x));
    this.wrappedStatement.setShort(parameterIndex, x);
  }
  @Override
  public void setInt(int parameterIndex, int x) throws SQLException {
    this.parameterValues.put(parameterIndex, Integer.toString(x));
    this.wrappedStatement.setInt(parameterIndex, x);
  }
  @Override
  public void setLong(int parameterIndex, long x) throws SQLException {
    this.parameterValues.put(parameterIndex, Long.toString(x));
    this.wrappedStatement.setLong(parameterIndex, x);
  }
  @Override
  public void setFloat(int parameterIndex, float x) throws SQLException {
    this.parameterValues.put(parameterIndex, Float.toString(x));
    this.wrappedStatement.setFloat(parameterIndex, x);
  }

  @Override
  public void setDouble(int parameterIndex, double x) throws SQLException {
    this.parameterValues.put(parameterIndex, Double.toString(x));
    this.wrappedStatement.setDouble(parameterIndex, x);
  }

  @Override
  public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
    this.parameterValues.put(parameterIndex, String.valueOf(x));
    this.wrappedStatement.setBigDecimal(parameterIndex, x);
  }
  @Override
  public void setString(int parameterIndex, String x) throws SQLException {
    this.parameterValues.put(parameterIndex, x);
    this.wrappedStatement.setString(parameterIndex, x);
  }
  @Override
  public void setBytes(int parameterIndex, byte[] x) throws SQLException {
    this.parameterValues.put(parameterIndex, Arrays.toString(x));
    this.wrappedStatement.setBytes(parameterIndex, x);
  }
  @Override
  public void setDate(int parameterIndex, Date x) throws SQLException {
    this.parameterValues.put(parameterIndex, DATE_FORMAT.get().format(x));
    this.wrappedStatement.setDate(parameterIndex, x);
  }
  @Override
  public void setTime(int parameterIndex, Time x) throws SQLException {
    this.parameterValues.put(parameterIndex, DATE_FORMAT.get().format(x));
    this.wrappedStatement.setTime(parameterIndex, x);
  }
  @Override
  public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
    this.parameterValues.put(parameterIndex, DATE_FORMAT.get().format(x));
    this.wrappedStatement.setTimestamp(parameterIndex, x);
  }
  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setObject(int parameterIndex, Object x) throws SQLException {
    if (x instanceof java.util.Date) {
      this.parameterValues.put(parameterIndex, DATE_FORMAT.get().format((java.util.Date) x));
    } else  if (x != null && x.getClass().isArray()) {
      this.parameterValues.put(parameterIndex, Arrays.toString((Object[]) x));
    } else {
      this.parameterValues.put(parameterIndex, String.valueOf(x));
    }
    this.wrappedStatement.setObject(parameterIndex, x);
  }

  @Override
  public void addBatch() throws SQLException {
    this.batchParameters.add(this.parameterValues);
    this.parameterValues = new HashMap<>();
    this.wrappedStatement.addBatch();
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setRef(int parameterIndex, Ref x) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setBlob(int parameterIndex, Blob x) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void setClob(int parameterIndex, Clob x) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void setArray(int parameterIndex, Array x) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public ResultSetMetaData getMetaData() throws SQLException {
    return this.wrappedStatement.getMetaData();
  }
  @Override
  public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("This method isn't implementd");
  }
  @Override
  public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("This method isn't implementd");
  }
  @Override
  public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("This method isn't implementd");
  }
  @Override
  public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
    this.parameterValues.put(parameterIndex, null);
    this.wrappedStatement.setNull(parameterIndex, sqlType, typeName);
  }
  @Override
  public void setURL(int parameterIndex, URL x) throws SQLException {
    this.parameterValues.put(parameterIndex, String.valueOf(x));
    this.wrappedStatement.setURL(parameterIndex, x);
  }
  @Override
  public ParameterMetaData getParameterMetaData() throws SQLException {
    return this.wrappedStatement.getParameterMetaData();
  }
  @Override
  public void setRowId(int parameterIndex, RowId x) throws SQLException {
    this.parameterValues.put(parameterIndex, String.valueOf(x));
    this.wrappedStatement.setRowId(parameterIndex, x);
  }
  @Override
  public void setNString(int parameterIndex, String value) throws SQLException {
    this.parameterValues.put(parameterIndex, value);
    this.wrappedStatement.setNString(parameterIndex, value);
  }
  @Override
  public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setNClob(int parameterIndex, NClob value) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void setClob(int parameterIndex, Reader reader) throws SQLException {
    this.wrappedStatement.setClob(parameterIndex, reader);
    this.parameterValues.put(parameterIndex, reader == null ? "NULL" : "<READER>");
  }
  @Override
  public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
    this.wrappedStatement.setBlob(parameterIndex, inputStream);
    this.parameterValues.put(parameterIndex, inputStream == null ? "NULL" : "<INPUT STREAM>");
  }
  @Override
  public void setNClob(int parameterIndex, Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }
  @Override
  public void close() throws SQLException {
    this.wrappedStatement.close();
  }
  @Override
  public int getMaxFieldSize() throws SQLException {
    return this.wrappedStatement.getMaxFieldSize();
  }

  @Override
  public void setMaxFieldSize(int max) throws SQLException {
    this.wrappedStatement.setMaxFieldSize(max);
  }

  @Override
  public int getMaxRows() throws SQLException {
    return this.wrappedStatement.getMaxRows();
  }

  @Override
  public void setMaxRows(int max) throws SQLException {
    this.wrappedStatement.setMaxRows(max);
  }

  @Override
  public void setEscapeProcessing(boolean enable) throws SQLException {
    this.wrappedStatement.setEscapeProcessing(enable);
  }

  @Override
  public int getQueryTimeout() throws SQLException {
    return this.wrappedStatement.getQueryTimeout();
  }

  @Override
  public void setQueryTimeout(int seconds) throws SQLException {
    this.wrappedStatement.setQueryTimeout(seconds);
  }

  @Override
  public void cancel() throws SQLException {
    throw new UnsupportedOperationException("This method isn't implementd.");
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    return this.wrappedStatement.getWarnings();
  }

  @Override
  public void clearWarnings() throws SQLException {
    this.wrappedStatement.clearWarnings();
  }

  @Override
  public void setCursorName(String name) throws SQLException {
    this.wrappedStatement.setCursorName(name);
  }

  @Override
  public ResultSet getResultSet() throws SQLException {
    return this.wrappedStatement.getResultSet();
  }

  @Override
  public int getUpdateCount() throws SQLException {
    return this.wrappedStatement.getUpdateCount();
  }

  @Override
  public boolean getMoreResults() throws SQLException {
    return this.wrappedStatement.getMoreResults();
  }

  @Override
  public void setFetchDirection(int direction) throws SQLException {
    this.wrappedStatement.setFetchDirection(direction);
  }

  @Override
  public int getFetchDirection() throws SQLException {
    return this.wrappedStatement.getFetchDirection();
  }

  @Override
  public void setFetchSize(int rows) throws SQLException {
    this.wrappedStatement.setFetchSize(rows);
  }

  @Override
  public int getFetchSize() throws SQLException {
    return this.wrappedStatement.getFetchSize();
  }

  @Override
  public int getResultSetConcurrency() throws SQLException {
    return this.wrappedStatement.getResultSetConcurrency();
  }

  @Override
  public int getResultSetType() throws SQLException {
    return this.wrappedStatement.getResultSetType();
  }

  @Override
  public void addBatch(String sql) throws SQLException {
    this.sqlTemplate.add(sql);
    this.wrappedStatement.addBatch(sql);
  }

  @Override
  public void clearParameters() throws SQLException {
    this.parameterValues.clear();
    this.batchParameters.clear();
    this.wrappedStatement.clearParameters();
  }
  @Override
  public void clearBatch() throws SQLException {
    this.batchParameters.clear();
    this.parameterValues.clear();
    this.sqlTemplate.clear();
    this.wrappedStatement.clearBatch();
  }

  @Override
  public Connection getConnection() throws SQLException {
    return this.wrappedStatement.getConnection();
  }

  @Override
  public boolean getMoreResults(int current) throws SQLException {
    return this.wrappedStatement.getMoreResults(current);
  }

  @Override
  public ResultSet getGeneratedKeys() throws SQLException {
    return this.wrappedStatement.getGeneratedKeys();
  }

  @Override
  public int getResultSetHoldability() throws SQLException {
    return this.wrappedStatement.getResultSetHoldability();
  }

  @Override
  public boolean isClosed() throws SQLException {
    return this.wrappedStatement.isClosed();
  }

  @Override
  public void setPoolable(boolean poolable) throws SQLException {
    this.wrappedStatement.setPoolable(poolable);
  }

  @Override
  public boolean isPoolable() throws SQLException {
    return this.wrappedStatement.isPoolable();
  }

  @Override
  public void closeOnCompletion() throws SQLException {
    this.wrappedStatement.closeOnCompletion();
  }

  @Override
  public boolean isCloseOnCompletion() throws SQLException {
    return this.wrappedStatement.isCloseOnCompletion();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return this.wrappedStatement.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return this.wrappedStatement.isWrapperFor(iface);
  }
}
