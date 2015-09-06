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
import cz.lbenda.dbapp.rc.User;
import cz.lbenda.dbapp.rc.UserImpl;
import cz.lbenda.dbapp.rc.db.audit.Auditor;
import cz.lbenda.dbapp.rc.db.audit.AuditorNone;
import cz.lbenda.dbapp.rc.db.audit.SqlLogToLogAuditor;
import cz.lbenda.dbapp.rc.db.audit.SqlLogToTableAuditor;
import cz.lbenda.dbapp.rc.db.dialect.SQLDialect;
import cz.lbenda.dbapp.rc.frm.RowNode;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import cz.lbenda.schema.dbapp.exconf.AuditType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class for reading data structure from JDBC
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public class DbStructureReader implements DBAppDataSource.DBAppDataSourceExceptionListener {

  private static final Logger LOG = LoggerFactory.getLogger(DbStructureReader.class);

  private SessionConfiguration sessionConfiguration;
  private User user; public User getUser() { return user; }

  private DataSource dataSource = null;

  public final void setSessionConfiguration(SessionConfiguration sessionConfiguration) {
    this.sessionConfiguration = sessionConfiguration;
    if (sessionConfiguration != null && sessionConfiguration.getJdbcConfiguration() != null) {
      user = new UserImpl(sessionConfiguration.getJdbcConfiguration().getUsername());
    }
    createDataSource();
  }
  @SuppressWarnings("unused")
  public final SessionConfiguration getSessionConfiguration() { return sessionConfiguration; }

  /** Inform if the reader is prepared for read data - the session configuration exist */
  public final boolean isPrepared() { return sessionConfiguration != null; }

  private void createDataSource() throws IllegalStateException {
    if (!isPrepared()) {
      throw new IllegalStateException("The DBStructureReader isn't yet prepared for create dataSource. Please check isPrepared() properties");
    }
    if (dataSource != null) { dataSource = null; }
    dataSource = new DBAppDataSource(sessionConfiguration);
  }

  public Connection getConnection() throws RuntimeException {
    if (dataSource == null) { createDataSource(); }
    try {
      Connection result = dataSource.getConnection();
      if (result == null) { throw new RuntimeException("The connection isn't created"); }
      return result;
    } catch (SQLException e) {
      LOG.error("Filed to get connection", e);
      throw new RuntimeException(e);
    }
  }

  /** Method read data from table to List, where every record is create as array of objects
   * @param td table which is readed
   * @param from first row where to start with read. If -1 is set, then return whole table
   * @param to last row where stop read
   * @return list of object from db
   */
  public final List<Object[]> readTableDate(TableDescription td, int from, int to) {
    try (Connection conn = getConnection()) {
      try (Statement st = conn.createStatement()) {
        ResultSet rs = st.executeQuery(String.format("SELECT * FROM \"%s\".\"%s\"", td.getSchema(), td.getName()));
        final List<Object[]> result;
        if (from > -1) { result = new ArrayList<>(to - from); }
        else { result = new ArrayList<>(); }
        while (rs.next()) {
          Object[] row = new Object[td.getColumns().size()];
          for (int i = 0; i < td.getColumns().size(); i++) { row[i] = rs.getObject(i + 1); }
          result.add(row);
        }
        return result;
      }
    } catch (SQLException e) {
      LOG.error(String.format("Problem with read whole table data: %s.%s, from: %s, to: %s", td.getSchema(), td.getName(), from, to), e);
      throw new RuntimeException(String.format("Problem with read whole table data: %s.%s, from: %s, to: %s", td.getSchema(), td.getName(), from, to), e);
    }
  }

  /** Method return data which are in joined table. The binding is defined by
   * <b>fk</b>.
   * @param fk foreign key which describe connection between tables
   * @param selectedRow all values of selected row
   * @return resul set or null
   */
  public final List<RowNode.Row> getJoinedRows(final ForeignKey fk, final RowNode.Row selectedRow) {
    final Object fkValue;
    final String tbSchema;
    final String tbName;
    final String tbColumn;
    final TableDescription td;
    if (selectedRow.getTableDescription().equals(fk.getMasterTable())) {
      fkValue = selectedRow.getValue(fk.getMasterColumn());
      tbSchema = fk.getSlaveTable().getSchema();
      tbName = fk.getSlaveTable().getName();
      tbColumn = fk.getSlaveColumn().getName();
      td = fk.getSlaveTable();
    } else {
      fkValue = selectedRow.getValue(fk.getSlaveColumn());
      tbSchema = fk.getMasterTable().getSchema();
      tbName = fk.getMasterTable().getName();
      tbColumn = fk.getMasterColumn().getName();
      td = fk.getMasterTable();
    }

    String sql = String.format("SELECT * FROM \"%s\".\"%s\" WHERE \"%s\"=?",
            tbSchema, tbName, tbColumn);
    LOG.debug(sql);
    try (Connection conn = getConnection()) {
      try (PreparedStatement ps = conn.prepareCall(sql)) {
        ps.setObject(1, fkValue);
        try (ResultSet rs =  ps.executeQuery()) {
          List<RowNode.Row> result = new ArrayList<>();
          while (rs.next()) {
            RowNode.Row row = new RowNode.Row(td);
            for (int i = 0; i < td.columnCount(); i++) {
              row.setValue(i, rs.getObject(i + 1));
            }
            result.add(row);
          }
          return result;
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void insertRow(TableDescription td, Map<Column, Object> newValues) {
    List<Column> changedColumns = new ArrayList<>();
    StringBuilder names = new StringBuilder();
    StringBuilder values = new StringBuilder();
    for (Column col : td.getColumns()) {
      Object newO = newValues.get(col);
      if (newO != null) { // Null value will be set to default
        if (!changedColumns.isEmpty()) {
          names.append(", ");
          values.append(", ");
        }
        names.append('"').append(col.getName()).append("\"");
        values.append('?');
        changedColumns.add(col);
      }
    }

    if (changedColumns.isEmpty()) {
      LOG.info("Nothing to insert");
    } else {
      String sql = String.format("insert into \"%s\".\"%s\" (%s) values (%s)", td.getSchema(), td.getName(), names, values);
      LOG.debug(sql);
      try (Connection conn = getConnection()) {
        try (PreparedStatement ps = AuditPreparedStatement.prepareStatement(user, auditorForAudit(td.getAudit()), conn,
            sql, Statement.RETURN_GENERATED_KEYS)) {
          int i = 1;
          for (Column col : changedColumns) {
            ps.setObject(i, newValues.get(col));
            i++;
          }

          ps.execute();
          ResultSet rs = ps.getGeneratedKeys();
          while (rs.next()) { // FIXME : This isn't the best solution, because there can be more auto generated fields and no every must be PK
            if (!td.getPKColumns().isEmpty()) {
              Column col = td.getPKColumns().get(0);
              newValues.put(col, rs.getObject(1));
            }
          }
          LOG.debug("New column was inserted");
          td.sqlWasFired(TableDescriptionExtension.TableAction.INSERT);
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /** Return auditor object for given audit type with configuration
   * @param auditType audit configuration
   * @return auditor, never return null */
  private Auditor auditorForAudit(AuditType auditType) {
    switch (auditType.getType()) {
      case NONE : return AuditorNone.getInstance();
      case SQL_LOG_TO_LOG: return SqlLogToLogAuditor.getInstance();
      case SQL_LOG_TO_TABLE : return SqlLogToTableAuditor.getInstance(this, auditType);
      default : return AuditorNone.getInstance();
    }
  }

  public void updateRow(TableDescription td, Map<Column, Object> oldValues, Map<Column, Object> newValues) {
    StringBuilder where = new StringBuilder();
    for (Column col : td.getPKColumns()) {
      if (where.length() != 0) { where.append(" an+"
              + "d "); }
      where.append('"').append(col.getName()).append("\"=?");
    }

    List<Column> changedColumns = new ArrayList<>();
    StringBuilder set = new StringBuilder();
    for (Column col : td.getColumns()) {
      Object oldO = oldValues.get(col);
      Object newO = newValues.get(col);
      if ((oldO == null && newO != null) || (oldO != null && !oldO.equals(newO))) { // Update only changes
        if (!changedColumns.isEmpty()) { set.append(", "); }
        set.append('"').append(col.getName()).append("\"=?");
        changedColumns.add(col);
      }
    }

    if (changedColumns.isEmpty()) {
      LOG.info("Nothing changed in form");
    } else {
      String sql = String.format("update \"%s\".\"%s\" set %s where %s", td.getSchema(), td.getName(), set, where);
      LOG.debug(sql);
      try (Connection conn = getConnection()) {
        try (PreparedStatement ps = AuditPreparedStatement.prepareCall(user, auditorForAudit(td.getAudit()), conn, sql)) {
          int i = 1;
          for (Column col : changedColumns) {
            ps.setObject(i, newValues.get(col));
            i++;
          }
          for (Column col : td.getPKColumns()) {
            ps.setObject(i, oldValues.get(col));
            i++;
          }
          ps.execute();
          LOG.debug("Changes was saved");
          td.sqlWasFired(TableDescriptionExtension.TableAction.UPDATE);
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /** Method which remove row from table. If table have primary key, then use primary key for delete row, if haven't
   * then use values of all columns. If there is table without any unique index (or primary key) then can be more then
   * one row deleted.
   * @param td table description
   * @param rowValues all values of column
   */
  public final void deleteRow(final TableDescription td, final Map<Column, Object> rowValues) {
    StringBuilder where = new StringBuilder();
    List<Column> pks = td.getPKColumns();
    if (pks.isEmpty()) { pks = td.getColumns(); }
    for (Column col : pks) {
      if (where.length() != 0) { where.append(", "); }
      where.append('"').append(col.getName()).append('"').append("=?");
    }
    String sql = String.format("DELETE FROM \"%s\".\"%s\" WHERE %s", td.getSchema(), td.getName(), where);
    LOG.debug(sql);
    try (Connection conn = getConnection()) {
      try (PreparedStatement ps = AuditPreparedStatement.prepareCall(user, auditorForAudit(td.getAudit()), conn, sql)) {
        int i = 1;
        for (Column col : pks) {
          ps.setObject(i, rowValues.get(col));
          i++;
        }
        ps.execute();
        LOG.debug("Record was deleted.");
        td.sqlWasFired(TableDescriptionExtension.TableAction.DELETE);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void generateStructure() {
    try (Connection conn = getConnection()) {
      DatabaseMetaData dmd = conn.getMetaData();
      SQLDialect dialect = this.sessionConfiguration.getJdbcConfiguration().getDialect();

      // List<TableDescription> result;
      try (ResultSet tabs = dmd.getTables(null, null, null, null)) {
        // result = new ArrayList<>();
        while (tabs.next()) {
          TableDescription td = this.sessionConfiguration.getOrCreateTableDescription(tabs.getString(dialect.tableCatalog()),
              tabs.getString(dialect.tableSchema()), tabs.getString(dialect.tableName()));
          td.setTableType(TableDescription.TableType.fromJDBC(tabs.getString(dialect.tableType())));
          td.setComment(dialect.tableRemarks());
        }
        generateStructureColumns(dmd);
        generatePKColumns(dmd);
        generateStrucutreForeignKeys(dmd);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void generateStructureColumns(DatabaseMetaData dmd) throws SQLException {
    SQLDialect di = this.sessionConfiguration.getJdbcConfiguration().getDialect();
    try (ResultSet rsColumn  = dmd.getColumns(null, null, null, null)) {
      ResultSetMetaData rsmd = rsColumn.getMetaData();
      for (int i = 1; i < rsmd.getColumnCount(); i++) {
        LOG.trace(String.format("%s: %s", rsmd.getColumnName(i), rsmd.getColumnType(i)));
      }
      while (rsColumn.next()) {
        TableDescription td = sessionConfiguration.getTableDescription(
                rsColumn.getString(di.columnTableCatalog()), rsColumn.getString(di.columnTableSchema()),
                rsColumn.getString(di.columnTableName()));
        Column column = new Column(td, rsColumn.getString(di.columnName()), rsColumn.getInt(di.columnDateType()),
                rsColumn.getInt(di.columnSize()), "YES".equals(rsColumn.getString(di.columnNullable())),
                "YES".equals(rsColumn.getString(di.columnAutoIncrement())),
                di.columnGenerated() != null ? "YES".equals(rsColumn.getString(di.columnGenerated())) : false);
        column.setComment(rsColumn.getString("REMARKS"));
        td.addColumn(column);
      }
    }
  }

  private void generatePKColumns(DatabaseMetaData dmd) throws SQLException {
    SQLDialect di = this.sessionConfiguration.getJdbcConfiguration().getDialect();
    for (TableDescription td : sessionConfiguration.getTableDescriptions()) {
      try (ResultSet rsPk = dmd.getPrimaryKeys(td.getCatalog(), td.getSchema(), td.getName())) {
        while (rsPk.next()) {
          Column column = td.getColumn(rsPk.getString(di.pkColumnName()));
          column.setPK(true);
        }
      }
    }
  }

  public final List<Object[]> getSQLRows(String sql, String... columnNames) throws SQLException {
    LOG.trace("load sql rows for SQL: " + sql);
    List<Object[]> result = new ArrayList<>();
    try (Statement stm = getConnection().createStatement()) {
      try (ResultSet rs = stm.executeQuery(sql)) {
        while (rs.next()) {
          Object[] row = new Object[columnNames.length];
          result.add(row);
          for (int i = 0; i < columnNames.length; i++) {
            row[i] = rs.getObject(columnNames[i]);
          }
        }
      }
    } catch (SQLException e) {
      LOG.error("Failed to load SQL: " + sql, e);
      throw e;
    }
    return result;
  }

  public final List<Object[]> getSQLRows(String sql, int... columnPoz) throws SQLException {
    LOG.trace("load sql rows for SQL: " + sql);
    List<Object[]> result = new ArrayList<>();
    try (Statement stm = getConnection().createStatement()) {
      try (ResultSet rs = stm.executeQuery(sql)) {
        while (rs.next()) {
          Object[] row = new Object[columnPoz.length];
          result.add(row);
          for (int i = 0; i < columnPoz.length; i++) {
            row[i] = rs.getObject(columnPoz[i]);
          }
        }
      }
    }
    return result;
  }

  private void generateStrucutreForeignKeys(DatabaseMetaData dmd) throws SQLException {
    SQLDialect di = this.sessionConfiguration.getJdbcConfiguration().getDialect();
    for (TableDescription td : sessionConfiguration.getTableDescriptions()) {
      ResultSet rsEx = dmd.getExportedKeys(td.getCatalog(), td.getSchema(), td.getName());
      while (rsEx.next()) {
        TableDescription slaveTD = sessionConfiguration.getTableDescription(rsEx.getString(di.fkSlaveTableCatalog()),
                rsEx.getString(di.fkSlaveTableSchema()), rsEx.getString(di.fkSlaveTableName()));
        ForeignKey fk = new ForeignKey(td, td.getColumn(rsEx.getString(di.fkMasterColumnName())),
                slaveTD, slaveTD.getColumn(rsEx.getString(di.fkSlaveColumnName())));
        td.addForeignKey(fk);
        slaveTD.addForeignKey(fk);
      }
    }
  }

  @Override
  public void onDBAppDataSourceException(Exception e) {
  }

  public static class ForeignKey {
    private final TableDescription masterTable; public final TableDescription getMasterTable() { return masterTable; }
    private final Column masterColumn; public final Column getMasterColumn() { return masterColumn; }
    private final TableDescription slaveTable; public final TableDescription getSlaveTable() { return slaveTable; }
    private final Column slaveColumn; public final Column getSlaveColumn() { return slaveColumn; }
    public ForeignKey(final TableDescription masterTable, final Column masterColumn,
            final TableDescription slaveTable, final Column slaveColumn) {
      this.masterTable = masterTable;
      this.masterColumn = masterColumn;
      this.slaveTable = slaveTable;
      this.slaveColumn = slaveColumn;
    }
  }
}
