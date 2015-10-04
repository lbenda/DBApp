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

import cz.lbenda.dataman.db.dialect.SQLDialect;

import java.sql.*;
import java.util.*;

import cz.lbenda.rcp.StatusHelper;
import cz.lbenda.rcp.localization.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/** Class for reading data structure from JDBC. The first method which must be call is
 * @author Lukas Benda <lbenda at lbenda.cz> */
public class DbStructureFactory implements DatamanDataSource.DBAppDataSourceExceptionListener {

  private static final Logger LOG = LoggerFactory.getLogger(DbStructureFactory.class);

  @Message
  public static final String TASK_NAME = "Read database structure";
  @Message
  public static final String STEP_READ_TABLES = "Read tables";
  @Message
  public static final String STEP_READ_COLUMNS = "Read columns";
  @Message
  public static final String STEP_READ_PRIMARY_KEYS = "Read primary keys";
  @Message
  public static final String STEP_READ_FOREIGN_KEYS = "Read foreign keys";
  @Message
  public static final String STEP_FINISH = "Structure of database was read.";

  private final ConnectionProvider connectionProvider;
  private final DbConfig dbConfig;

  public DbStructureFactory(@Nonnull DbConfig dbConfig) {
    // this.setDbConfig(dbConfig);
    this.dbConfig = dbConfig;
    this.connectionProvider = dbConfig.getConnectionProvider();
  }

  /** Method read data from table to List, where every record is create as array of objects
   * @param td table which is read
   * @param from first row where to start with read. If -1 is set, then return whole table
   * @param to last row where stop read
   * @return list of object from db */
  public final List<RowDesc> readTableData(TableDesc td, int from, int to) {
    try (Connection conn = connectionProvider.getConnection()) {
      try (Statement st = conn.createStatement()) {
        String sql = String.format("SELECT * FROM \"%s\".\"%s\"", td.getSchema(), td.getName());
        ResultSet rs = st.executeQuery(sql);
        final List<RowDesc> result;
        if (from > -1) { result = new ArrayList<>(to - from); }
        else { result = new ArrayList<>(); }
        while (rs.next()) {
          RowDesc row = RowDesc.createNewRow(td.getQueryRow().getMetaData(), RowDesc.RowDescState.LOADED);
          for (ColumnDesc columnDesc : td.getColumns()) {
            row.loadInitialColumnValue(columnDesc, rs);
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
   * @return resul set or null */
  @SuppressWarnings("unused")
  public final List<TableRow> getJoinedRows(final ForeignKey fk, final TableRow selectedRow) {
    final Object fkValue;
    final SchemaDesc tbSchema;
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
            tbSchema.getName(), tbName, tbColumn);
    LOG.debug(sql);
    try (Connection conn = connectionProvider.getConnection()) {
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

  private static final double progressStepCount = 4;

  public void generateStructure() {
    try (Connection conn = connectionProvider.getConnection()) {
      Map<String, CatalogDesc> catalogs = new HashMap<>();

      DatabaseMetaData dmd = conn.getMetaData();
      SQLDialect dialect = dbConfig.getDialect();
      StatusHelper.getInstance().progressStart(this, TASK_NAME, progressStepCount);
      try (ResultSet tabs = dmd.getTables(null, null, null, null)) {
        tabs.last();
        StatusHelper.getInstance().progressNextStep(this, STEP_READ_TABLES, tabs.getRow());
      } catch (SQLException e) {
        StatusHelper.getInstance().progressNextStep(this, STEP_READ_TABLES, 200);
      }
      try (ResultSet tabs = dmd.getTables(null, null, null, null)) {
        while (tabs.next()) {
          StatusHelper.getInstance().progress(this);
          String catalogName = tabs.getString(dialect.tableCatalog());
          String schemaName = tabs.getString(dialect.tableSchema());
          String tableName = tabs.getString(dialect.tableName());
          CatalogDesc catalogDesc = catalogs.get(catalogName);
          if (catalogDesc == null) {
            catalogDesc = new CatalogDesc(catalogName);
            catalogs.put(catalogDesc.getName(), catalogDesc);
          }
          SchemaDesc schema = catalogDesc.getSchema(schemaName);
          if (schema == null) {
            schema = new SchemaDesc(catalogDesc, schemaName);
            catalogDesc.getSchemas().add(schema);
          }
          TableDesc tableDesc = schema.getTable(tableName);
          if (tableDesc == null) {
            tableDesc = new TableDesc(schema, tabs.getString(dialect.tableType()), tableName);
            tableDesc.setDbConfig(dbConfig);
            schema.getTables().add(tableDesc);
          }
          tableDesc.setSavableRegister(connectionProvider.getSavableRegistry());
          tableDesc.setComment(dialect.tableRemarks());
        }
        generateStructureColumns(catalogs::get, dmd);
        generatePKColumns(catalogs.values(), dmd);
        generateStructureForeignKeys(catalogs, dmd);
        dbConfig.getCatalogs().clear();
        dbConfig.getCatalogs().addAll(catalogs.values());
        StatusHelper.getInstance().progressFinish(this, STEP_FINISH);
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

  private void generateStructureColumns(CatalogHolder catalogHolder, DatabaseMetaData dmd) throws SQLException {
    SQLDialect dialect = dbConfig.getJdbcConfiguration().getDialect();
    try (ResultSet rsColumn  = dmd.getColumns(null, null, null, null)) {
      rsColumn.last();
      StatusHelper.getInstance().progressNextStep(this, STEP_READ_COLUMNS, rsColumn.getRow());
    } catch (SQLException e) {
      StatusHelper.getInstance().progressNextStep(this, STEP_READ_COLUMNS, 500);
    }
    try (ResultSet rsColumn  = dmd.getColumns(null, null, null, null)) {

      writeColumnNames(rsColumn.getMetaData());
      while (rsColumn.next()) {
        StatusHelper.getInstance().progress(this);
        String catalog = rsColumn.getString(dialect.columnTableCatalog());
        String schema = rsColumn.getString(dialect.columnTableSchema());
        String table = rsColumn.getString(dialect.columnTableName());
        TableDesc td = catalogHolder.getCatalog(catalog).getSchema(schema).getTable(table);
        ColumnDesc column = new ColumnDesc(td, rsColumn, dialect);
        td.addColumn(column);
      }
    }
  }

  private void generatePKColumns(Collection<CatalogDesc> catalogs, DatabaseMetaData dmd) throws SQLException {
    SQLDialect di = dbConfig.getJdbcConfiguration().getDialect();

    StatusHelper.getInstance().progressNextStep(this, STEP_READ_PRIMARY_KEYS,
        catalogs.stream()
            .mapToInt(cat -> cat.getSchemas().stream()
                .mapToInt(schema -> schema.getTables().size()).sum()).sum());
    for (CatalogDesc ch : catalogs) {
      for (SchemaDesc schema : ch.getSchemas()) {
        for (TableDesc td : schema.getTables()) {
          StatusHelper.getInstance().progress(this);
          try (ResultSet rsPk = dmd.getPrimaryKeys(ch.getName(), schema.getName(), td.getName())) {
            while (rsPk.next()) {
              ColumnDesc column = td.getColumn(rsPk.getString(di.pkColumnName()));
              if (column == null) {
                LOG.error("The primary column not exist in whole column set of table: " + di.pkColumnName());
              } else {
                column.setPK(true);
              }
            }
          }
        }
      }
    }
  }

  public final List<Object[]> getSQLRows(String sql, String... columnNames) throws SQLException {
    LOG.trace("load sql rows for SQL: " + sql);
    List<Object[]> result = new ArrayList<>();
    try (Statement stm = connectionProvider.getConnection().createStatement()) {
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
    try (Statement stm = connectionProvider.getConnection().createStatement()) {
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

  private void generateStructureForeignKeys(Map<String, CatalogDesc> catalogs, DatabaseMetaData dmd) throws SQLException {
    SQLDialect di = dbConfig.getJdbcConfiguration().getDialect();
    StatusHelper.getInstance().progressNextStep(this, STEP_READ_FOREIGN_KEYS,
        catalogs.values().stream()
            .mapToInt(cat -> cat.getSchemas().stream()
                .mapToInt(schema -> schema.getTables().size()).sum()).sum());
    for (CatalogDesc ch : catalogs.values()) {
      for (SchemaDesc schema : ch.getSchemas()) {
        for (TableDesc td : schema.getTables()) {
          StatusHelper.getInstance().progress(this);
          ResultSet rsEx = dmd.getExportedKeys(ch.getName(), schema.getName(), td.getName());
          while (rsEx.next()) {
            String slaveCatalogName = rsEx.getString(di.fkSlaveTableCatalog());
            String slaveSchemaName = rsEx.getString(di.fkSlaveTableSchema());
            String slaveTableName = rsEx.getString(di.fkSlaveTableName());
            TableDesc slaveTD = catalogs.get(slaveCatalogName).getSchema(slaveSchemaName).getTable(slaveTableName);
            ForeignKey fk = new ForeignKey(td, td.getColumn(rsEx.getString(di.fkMasterColumnName())),
                slaveTD, slaveTD.getColumn(rsEx.getString(di.fkSlaveColumnName())));
            td.addForeignKey(fk);
            slaveTD.addForeignKey(fk);
          }
        }
      }
    }
  }

  @Override
  public void onDBAppDataSourceException(Exception e) {
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

  @FunctionalInterface
  private interface CatalogHolder {
    CatalogDesc getCatalog(String name);
  }
}
