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

import cz.lbenda.common.Tuple2;
import cz.lbenda.dataman.rc.DbConfig;
import cz.lbenda.dataman.User;
import cz.lbenda.dataman.UserImpl;
import cz.lbenda.dataman.db.audit.Auditor;
import cz.lbenda.dataman.db.audit.AuditorNone;
import cz.lbenda.dataman.db.audit.SqlLogToLogAuditor;
import cz.lbenda.dataman.db.audit.SqlLogToTableAuditor;
import cz.lbenda.dataman.db.dialect.SQLDialect;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cz.lbenda.dataman.schema.exconf.AuditType;
import cz.lbenda.rcp.ExceptionMessageFrmController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class for reading data structure from JDBC. The first method which must be call is
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public class DbStructureReader implements DBAppDataSource.DBAppDataSourceExceptionListener, SQLExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(DbStructureReader.class);

  private DbConfig dbConfig;
  private User user; public User getUser() { return user; }

  private DBAppDataSource dataSource = null;
  private boolean connected = false;

  public final void setDbConfig(DbConfig dbConfig) {
    this.dbConfig = dbConfig;
    if (dbConfig != null && dbConfig.getJdbcConfiguration() != null) {
      user = new UserImpl(dbConfig.getJdbcConfiguration().getUsername());
    }
    createDataSource();
  }
  @SuppressWarnings("unused")
  public final DbConfig getDbConfig() { return dbConfig; }

  public DbStructureReader(DbConfig dbConfig) {
    this.setDbConfig(dbConfig);
  }

  /** Inform if the reader is prepared for read data - the session configuration exist */
  public final boolean isPrepared() { return dbConfig != null; }
  public final boolean isConnected() { return connected; }

  private void createDataSource() throws IllegalStateException {
    if (!isPrepared()) {
      throw new IllegalStateException("The DBStructureReader isn't yet prepared for create dataSource. Please check isPrepared() properties");
    }
    if (dataSource != null) { dataSource = null; }
    dataSource = new DBAppDataSource(dbConfig);
  }

  /** Close all connections */
  public void close() {
    if (dataSource != null) {
      try {
        dataSource.closeAllConnections();
        this.connected = false;
      } catch (SQLException e) {
        LOG.error("The connection isn't close.", e);
      }
    } else { this.connected = false; }
  }

  public Connection getConnection() throws RuntimeException {
    if (dataSource == null) { createDataSource(); }
    try {
      Connection result = dataSource.getConnection();
      if (result == null) { throw new RuntimeException("The connection isn't created"); }
      this.connected = !result.isClosed();
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
  public final List<Object[]> readTableDate(TableDesc td, int from, int to) {
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
  public final List<TableRow> getJoinedRows(final ForeignKey fk, final TableRow selectedRow) {
    final Object fkValue;
    final String tbSchema;
    final String tbName;
    final String tbColumn;
    final TableDesc td;
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
          List<TableRow> result = new ArrayList<>();
          while (rs.next()) {
            TableRow row = new TableRow(td);
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

  public ResultSet executeSQL(String sql) {
    try (Connection conn = getConnection()) {
      try (PreparedStatement ps = conn.prepareCall(sql)) { // FIXME auditing SQL
        boolean isResultSet = ps.execute();
        if (isResultSet) { return ps.getResultSet(); }
        return null; // FIXME
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
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

  public void insertRows(TableDesc td) {
    List<RowDesc> insertRows = new ArrayList<>();
    td.getRows().stream().filter(row -> RowDesc.RowDescState.NEW.equals(row.getState())).forEach(insertRows::add);
    if (insertRows.isEmpty()) { return; }

    StringBuilder names = new StringBuilder();
    StringBuilder values = new StringBuilder();
    List<ColumnDesc> insertedColumns = new ArrayList<>();
    for (ColumnDesc col : td.getColumns()) {
      if (!col.isAutoincrement() && !col.isGenerated()) {
        if (!insertedColumns.isEmpty()) {
          names.append(", ");
          values.append(", ");
        }
        insertedColumns.add(col);
        names.append('"').append(col.getName()).append("\"");
        values.append('?');
      }
    }

    String sql = String.format("insert into \"%s\".\"%s\" (%s) values (%s)", td.getSchema(), td.getName(), names, values);
    LOG.debug(sql);
    try (Connection conn = getConnection()) {
      try (PreparedStatement ps = AuditPreparedStatement.prepareStatement(user, auditorForAudit(td.getAudit()), conn,
          sql, Statement.RETURN_GENERATED_KEYS)) {
        for (RowDesc row : insertRows) {
          int i = 1;
          for (ColumnDesc col : insertedColumns) {
            ps.setObject(i, row.getNewValues()[col.getPosition()]);
            i++;
          }
          ps.addBatch();
        }
        ps.executeBatch();
        ResultSet rs = ps.getGeneratedKeys();
        ResultSetMetaData rsmd = rs.getMetaData();
        Iterator<RowDesc> itt = insertRows.iterator();
        while (rs.next()) {
          RowDesc row = itt.next();
          for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            ColumnDesc col = td.getColumn(rsmd.getColumnName(i));
            row.getNewValues()[col.getPosition()] = rs.getObject(i);
          }
        }
        insertRows.forEach(RowDesc::savedChanges);
        LOG.debug("New column was inserted");
      }
    } catch (SQLException e) {
      LOG.error("Problem with save inserted row", e);
      ExceptionMessageFrmController.showException(e);
    }
  }

  public void updateRows(TableDesc td) {
    List<RowDesc> changedRows = new ArrayList<>();
    td.getRows().stream().filter(row -> RowDesc.RowDescState.CHANGED.equals(row.getState())).forEach(changedRows::add);
    if (changedRows.isEmpty()) { return; }

    final StringBuilder where = new StringBuilder();
    List<ColumnDesc> pks = td.getPKColumns();
    if (pks.isEmpty()) { pks = td.getColumns(); }
    pks.stream().forEachOrdered(col -> {
      if (where.length() != 0) { where.append(" and "); }
      where.append('"').append(col.getName()).append("\"=?");
    });
    StringBuilder set = new StringBuilder();
    td.getColumns().stream().forEachOrdered(col -> {
      if (set.length() > 0) {
        set.append(", ");
      }
      set.append('"').append(col.getName()).append("\"=?");
    });
    String sql = String.format("update \"%s\".\"%s\" set %s where %s", td.getSchema(), td.getName(), set, where);
    LOG.debug(sql);

    try (Connection conn = getConnection()) {
      try (PreparedStatement ps = AuditPreparedStatement.prepareCall(user, auditorForAudit(td.getAudit()), conn, sql)) {
        for (RowDesc row : changedRows) {
          int i = 0;
          for (; i < row.getNewValues().length; i++) {
            ps.setObject(i + 1, row.getNewValues()[i]);
          }
          for (ColumnDesc col : pks) {
            ps.setObject(i + 1, row.getOldValues()[col.getPosition()]);
            i++;
          }
          ps.addBatch();
        }
        ps.executeBatch();
        changedRows.forEach(RowDesc::savedChanges);
      }
    } catch (SQLException e) {
      LOG.error("Problem with update rows", e);
      ExceptionMessageFrmController.showException(e);
    }
  }

  /** Method which remove row from table. If table have primary key, then use primary key for delete row, if haven't
   * then use values of all columns. If there is table without any unique index (or primary key) then can be more then
   * one row deleted.
   * @param td table description */
  public final void deleteRows(final TableDesc td) {
    List<RowDesc> removedRows = new ArrayList<>();
    td.getRows().stream().filter(row -> RowDesc.RowDescState.REMOVED.equals(row.getState())).forEach(removedRows::add);
    if (removedRows.isEmpty()) { return; }

    StringBuilder where = new StringBuilder();
    List<ColumnDesc> pks = td.getPKColumns();
    if (pks.isEmpty()) { pks = td.getColumns(); }
    for (ColumnDesc col : pks) {
      if (where.length() != 0) { where.append(" and "); }
      where.append('"').append(col.getName()).append('"').append("=?");
    }
    String sql = String.format("DELETE FROM \"%s\".\"%s\" WHERE %s", td.getSchema(), td.getName(), where);

    LOG.debug(sql);
    try (Connection conn = getConnection()) {
      try (PreparedStatement ps = AuditPreparedStatement.prepareCall(user, auditorForAudit(td.getAudit()), conn, sql)) {
        for (RowDesc row : removedRows) {
          int i = 1;
          for (ColumnDesc col : pks) {
            ps.setObject(i, row.getOldValues()[col.getPosition()]);
            i++;
          }
          ps.addBatch();
        }
        ps.executeBatch();
        td.getRows().removeAll(removedRows);
        LOG.debug("Record was deleted.");
      }
    } catch (SQLException e) {
      LOG.error("Problem with delete rows", e);
      ExceptionMessageFrmController.showException(e);
    }
  }

  public void generateStructure() {
    try (Connection conn = getConnection()) {
      DatabaseMetaData dmd = conn.getMetaData();
      SQLDialect dialect = this.dbConfig.getJdbcConfiguration().getDialect();

      // List<TableDescription> result;
      try (ResultSet tabs = dmd.getTables(null, null, null, null)) {
        // result = new ArrayList<>();
        while (tabs.next()) {
          TableDesc td = this.dbConfig.getOrCreateTableDescription(tabs.getString(dialect.tableCatalog()),
              tabs.getString(dialect.tableSchema()), tabs.getString(dialect.tableName()));
          td.setTableType(TableDesc.TableType.fromJDBC(tabs.getString(dialect.tableType())));
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
    SQLDialect di = this.dbConfig.getJdbcConfiguration().getDialect();
    try (ResultSet rsColumn  = dmd.getColumns(null, null, null, null)) {
      ResultSetMetaData rsmd = rsColumn.getMetaData();
      for (int i = 1; i < rsmd.getColumnCount(); i++) {
        LOG.trace(String.format("%s: %s", rsmd.getColumnName(i), rsmd.getColumnType(i)));
      }
      while (rsColumn.next()) {
        TableDesc td = dbConfig.getTableDescription(
                rsColumn.getString(di.columnTableCatalog()), rsColumn.getString(di.columnTableSchema()),
                rsColumn.getString(di.columnTableName()));
        ColumnDesc column = new ColumnDesc(td, rsColumn.getString(di.columnName()), rsColumn.getString("REMARKS"),
            rsColumn.getInt(di.columnDateType()),
                rsColumn.getInt(di.columnSize()), "YES".equals(rsColumn.getString(di.columnNullable())),
                "YES".equals(rsColumn.getString(di.columnAutoIncrement())),
                di.columnGenerated() != null ? "YES".equals(rsColumn.getString(di.columnGenerated())) : false);
        td.addColumn(column);
      }
    }
  }

  private void generatePKColumns(DatabaseMetaData dmd) throws SQLException {
    SQLDialect di = this.dbConfig.getJdbcConfiguration().getDialect();
    for (TableDesc td : dbConfig.getTableDescriptions()) {
      try (ResultSet rsPk = dmd.getPrimaryKeys(td.getCatalog(), td.getSchema(), td.getName())) {
        while (rsPk.next()) {
          ColumnDesc column = td.getColumn(rsPk.getString(di.pkColumnName()));
          if (column == null) {
            LOG.error("The primary column not exist in whole column set of table: " + di.pkColumnName());
          } else { column.setPK(true); }
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
    SQLDialect di = this.dbConfig.getJdbcConfiguration().getDialect();
    for (TableDesc td : dbConfig.getTableDescriptions()) {
      ResultSet rsEx = dmd.getExportedKeys(td.getCatalog(), td.getSchema(), td.getName());
      while (rsEx.next()) {
        TableDesc slaveTD = dbConfig.getTableDescription(rsEx.getString(di.fkSlaveTableCatalog()),
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

  @Override
  public void onPreparedStatement(String sql, Consumer<Tuple2<PreparedStatement, SQLException>> consumer) {
    try (Connection connection = getConnection()) {
      try (PreparedStatement ps = connection.prepareCall(sql)) {
        consumer.accept(new Tuple2<>(ps, null));
      }
    } catch (SQLException e) {
      consumer.accept(new Tuple2<>(null, e));
    }
  }

  public static class ForeignKey {
    private final TableDesc masterTable; public final TableDesc getMasterTable() { return masterTable; }
    private final ColumnDesc masterColumn; public final ColumnDesc getMasterColumn() { return masterColumn; }
    private final TableDesc slaveTable; public final TableDesc getSlaveTable() { return slaveTable; }
    private final ColumnDesc slaveColumn; public final ColumnDesc getSlaveColumn() { return slaveColumn; }
    public ForeignKey(final TableDesc masterTable, final ColumnDesc masterColumn,
            final TableDesc slaveTable, final ColumnDesc slaveColumn) {
      this.masterTable = masterTable;
      this.masterColumn = masterColumn;
      this.slaveTable = slaveTable;
      this.slaveColumn = slaveColumn;
    }
  }
}
