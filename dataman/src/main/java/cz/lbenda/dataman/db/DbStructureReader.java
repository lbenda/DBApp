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
import cz.lbenda.dataman.db.dialect.SQLDialect;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import cz.lbenda.rcp.action.SavableRegistry;
import javafx.stage.Stage;
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
  /** Savable register for whole db config. */
  private SavableRegistry savableRegistry; public SavableRegistry getSavableRegistry() { return savableRegistry; }

  public final void setDbConfig(DbConfig dbConfig) {
    this.dbConfig = dbConfig;
    if (dbConfig != null && dbConfig.getJdbcConfiguration() != null) {
      user = new UserImpl(dbConfig.getJdbcConfiguration().getUsername());
    }
    createDataSource();
  }

  public DbStructureReader(DbConfig dbConfig) { this.setDbConfig(dbConfig); }

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
  public void close(Stage stage) {
    if (this.savableRegistry != null) {
      if (!this.savableRegistry.close(stage)) { return; }
      this.savableRegistry = null;
    }
    if (dataSource != null) {
      try {
        dataSource.closeAllConnections();
        this.connected = false;
      } catch (SQLException e) {
        LOG.error("The connection isn't close.", e);
      }
    } else {
      this.connected = false;
    }
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
  public final List<RowDesc> readTableData(TableDesc td, int from, int to) {
    try (Connection conn = getConnection()) {
      try (Statement st = conn.createStatement()) {
        ResultSet rs = st.executeQuery(String.format("SELECT * FROM \"%s\".\"%s\"", td.getSchema(), td.getName()));
        final List<RowDesc> result;
        if (from > -1) { result = new ArrayList<>(to - from); }
        else { result = new ArrayList<>(); }
        while (rs.next()) {
          RowDesc row = RowDesc.createNewRow(td.getQueryRow().getMetaData(), RowDesc.RowDescState.LOADED);
          for (ColumnDesc columnDesc : td.getColumns()) {
            row.setInitialColumnValue(columnDesc, rs.getObject(columnDesc.getPosition()));
          }
          result.add(row);
        }
        return result;
      }
    } catch (SQLException e) {
      LOG.error(String.format("Problem with read whole table data: %s.%s, from: %s, to: %s", td.getSchema(),
          td.getName(), from, to), e);
      throw new RuntimeException(String.format("Problem with read whole table data: %s.%s, from: %s, to: %s",
          td.getSchema(), td.getName(), from, to), e);
    }
  }

  /** Method return data which are in joined table. The binding is defined by
   * <b>fk</b>.
   * @param fk foreign key which describe connection between tables
   * @param selectedRow all values of selected row
   * @return resul set or null
   */
  @SuppressWarnings("unused")
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

  public SQLDialect getDialect() {
    return this.dbConfig.getJdbcConfiguration().getDialect();
  }

  public void generateStructure() {
    if (this.savableRegistry == null) { this.savableRegistry = SavableRegistry.getInstance(); }
    try (Connection conn = getConnection()) {
      DatabaseMetaData dmd = conn.getMetaData();
      SQLDialect dialect = getDialect();
      try (ResultSet tabs = dmd.getTables(null, null, null, null)) {
        while (tabs.next()) {
          TableDesc td = this.dbConfig.getOrCreateTableDescription(tabs.getString(dialect.tableCatalog()),
              tabs.getString(dialect.tableSchema()), tabs.getString(dialect.tableName()));
          td.setSavableRegister(savableRegistry);
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

  private void writeColumnNames(ResultSetMetaData metaData) throws SQLException {
    for (int i = 1; i <= metaData.getColumnCount(); i++) {
      System.out.print(metaData.getColumnName(i));
      System.out.print(" : ");
      System.out.println(metaData.getColumnLabel(i));
    }
  }

  private void generateStructureColumns(DatabaseMetaData dmd) throws SQLException {
    SQLDialect dialect = this.dbConfig.getJdbcConfiguration().getDialect();
    try (ResultSet rsColumn  = dmd.getColumns(null, null, null, null)) {
      writeColumnNames(rsColumn.getMetaData());
      while (rsColumn.next()) {
        TableDesc td = dbConfig.getTableDescription(
                rsColumn.getString(dialect.columnTableCatalog()), rsColumn.getString(dialect.columnTableSchema()),
                rsColumn.getString(dialect.columnTableName()));
        ColumnDesc column = new ColumnDesc(td, rsColumn, dialect);
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
