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

import cz.lbenda.common.Constants;
import cz.lbenda.dataman.db.dialect.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import cz.lbenda.dataman.schema.dataman.*;
import cz.lbenda.dataman.schema.dataman.ColumnType;
import cz.lbenda.dataman.schema.datatypes.DataTypeType;
import cz.lbenda.rcp.StatusHelper;
import cz.lbenda.rcp.localization.Message;
import javafx.application.Platform;
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
  private final Set<String> columnsFromWriten = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
   * @return result set or null */
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
        Platform.runLater(() -> dbConfig.getCatalogs().addAll(catalogs.values()));
        StatusHelper.getInstance().progressFinish(this, STEP_FINISH);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeColumnNames(String columnsFrom, ResultSetMetaData metaData) throws SQLException {
    if (Constants.IS_IN_DEVELOP_MODE) {
      if (!columnsFromWriten.contains(columnsFrom)) {
        LOG.debug("Write column names: " + columnsFrom);
        columnsFromWriten.add(columnsFrom);
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
          LOG.debug("Column: " + metaData.getColumnName(i) + " : "  + metaData.getColumnLabel(i));
        }
      }
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

      writeColumnNames("generateStructureColumns", rsColumn.getMetaData());
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
          writeColumnNames("generateStructureForeignKeys", rsEx.getMetaData());
          while (rsEx.next()) {
            String slaveCatalogName = rsEx.getString(di.fkSlaveTableCatalog());
            String slaveSchemaName = rsEx.getString(di.fkSlaveTableSchema());
            String slaveTableName = rsEx.getString(di.fkSlaveTableName());
            TableDesc slaveTD = catalogs.get(slaveCatalogName).getSchema(slaveSchemaName).getTable(slaveTableName);
            //noinspection ConstantConditions
            ForeignKey fk = new ForeignKey(rsEx.getString(di.fkName()),
                td, td.getColumn(rsEx.getString(di.fkMasterColumnName())),
                slaveTD, slaveTD.getColumn(rsEx.getString(di.fkSlaveColumnName())),
                rsEx.getString(di.fkUpdateRule()), rsEx.getString(di.fkDeleteRule()));
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

  public static List<CatalogDesc> loadDatabaseStructureFromXML(DatabaseStructureType databaseStructure) {
    List<CatalogDesc> result = new ArrayList<>();
    if (databaseStructure == null) { return result; }
    Map<Object, TableDesc> tableDescFromTableType = new HashMap<>();
    Map<Object, ColumnDesc> columnDescFromColumnType = new HashMap<>();

    databaseStructure.getCatalog().forEach(catalogType -> {
      CatalogDesc catalogDesc = new CatalogDesc(catalogType.getName());
      result.add(catalogDesc);
      catalogType.getSchema().forEach(schemaType -> {
        SchemaDesc schemaDesc = new SchemaDesc(catalogDesc, schemaType.getName());
        catalogDesc.getSchemas().add(schemaDesc);
        schemaType.getTable().forEach(tableType -> {
          TableDesc tableDesc = new TableDesc(schemaDesc, tableType.getTableType(), tableType.getName());
          tableDescFromTableType.put(tableType, tableDesc);
          schemaDesc.getTables().add(tableDesc);
          tableType.getColumn().forEach(columnType -> {
            ColumnDesc columnDesc = new ColumnDesc(tableDesc,
                columnType.getName(), columnType.getLabel(), dataTypeTypeToColumnType(columnType.getDataType()),
                columnType.getSize(), columnType.getScale(), columnType.isNullable(), columnType.isAutoincrement(),
                columnType.isGenerated(), columnType.getDefaultValue());
            columnDescFromColumnType.put(columnType, columnDesc);
            columnDesc.setPosition(tableDesc.getColumns().size() + 1);
            columnDesc.setPK(columnType.isIsPK());
            tableDesc.getColumns().add(columnDesc);
          });
        });
      });
    });

    databaseStructure.getCatalog().forEach(catalogType -> catalogType.getSchema().forEach(schemaType -> schemaType.getTable().forEach(tableType ->
        tableType.getForeignKey().forEach(foreignKeyType -> {
          ForeignKey foreignKey = new ForeignKey(
              foreignKeyType.getName(),
              tableDescFromTableType.get(foreignKeyType.getMasterTable()),
              columnDescFromColumnType.get(foreignKeyType.getMasterColumn().get(0).getColumn()),
              tableDescFromTableType.get(tableType),
              columnDescFromColumnType.get(foreignKeyType.getSlaveColumn().get(0).getColumn()),
              foreignKeyType.getUpdateRule(),
              foreignKeyType.getDeleteRule());
          foreignKey.getMasterTable().getForeignKeys().add(foreignKey);
          foreignKey.getSlaveTable().getForeignKeys().add(foreignKey);
        }))));
    return result;
  }

  public static DatabaseStructureType createXMLDatabaseStructure(List<CatalogDesc> catalogs) {
    ObjectFactory of = new ObjectFactory();
    DatabaseStructureType databaseStructureType = of.createDatabaseStructureType();
    Map<TableDesc, TableType> tableTypeForTableDesc = new HashMap<>();
    Map<ColumnDesc, ColumnType> columnTypeForColumnDesc = new HashMap<>();

    catalogs.forEach(catalogDesc -> {
      CatalogType catalogType = of.createCatalogType();
      catalogType.setName(catalogDesc.getName());
      databaseStructureType.getCatalog().add(catalogType);
      catalogDesc.getSchemas().forEach(schemaDesc -> {
        SchemaType schemaType = of.createSchemaType();
        schemaType.setName(schemaDesc.getName());
        catalogType.getSchema().add(schemaType);
        schemaDesc.getTables().forEach(tableDesc -> {
          TableType tableType = of.createTableType();
          tableTypeForTableDesc.put(tableDesc, tableType);
          tableType.setId(UUID.randomUUID().toString());
          tableType.setName(tableDesc.getName());
          tableType.setTableType(tableDesc.getTableType().name());
          schemaType.getTable().add(tableType);
          tableDesc.getColumns().forEach(columnDesc -> {
            ColumnType columnType = of.createColumnType();
            columnTypeForColumnDesc.put(columnDesc, columnType);
            columnType.setId(UUID.randomUUID().toString());
            columnType.setName(columnDesc.getName());
            columnType.setLabel(columnDesc.getLabel());
            columnType.setAutoincrement(columnDesc.isAutoincrement());
            columnType.setNullable(columnDesc.isNullable());
            columnType.setDataType(columnTypeToDataTypeType(columnDesc.getDataType()));
            columnType.setGenerated(columnDesc.isGenerated());
            columnType.setIsPK(columnDesc.isPK());
            columnType.setSize(columnDesc.getSize());
            columnType.setScale(columnDesc.getScale());
            columnType.setDefaultValue(columnDesc.getDefaultValue());
            tableType.getColumn().add(columnType);
          });
        });
      });
    });

    catalogs.forEach(catalogDesc -> catalogDesc.getSchemas().forEach(schemaDesc -> schemaDesc.getTables().forEach(tableDesc ->
      tableDesc.getForeignKeys().stream().filter(foreignKey -> foreignKey.getSlaveTable() == tableDesc).forEach(foreignKey -> {
        ForeignKeyType foreignKeyType = of.createForeignKeyType();
        foreignKeyType.setName(foreignKey.getName());
        foreignKeyType.setDeleteRule(foreignKey.getDeleteRule());
        foreignKeyType.setUpdateRule(foreignKey.getUpdateRule());

        foreignKeyType.setMasterTable(tableTypeForTableDesc.get(foreignKey.getMasterTable()));
        ForeignKeyColumnType masterColumn = of.createForeignKeyColumnType();
        masterColumn.setColumn(columnTypeForColumnDesc.get(foreignKey.getMasterColumn()));
        foreignKeyType.getMasterColumn().add(masterColumn);
        ForeignKeyColumnType slaveColumn = of.createForeignKeyColumnType();
        slaveColumn.setColumn(columnTypeForColumnDesc.get(foreignKey.getSlaveColumn()));
        foreignKeyType.getSlaveColumn().add(slaveColumn);

        tableTypeForTableDesc.get(tableDesc).getForeignKey().add(foreignKeyType);
      })
    )));
    return databaseStructureType;
  }

  public static DataTypeType columnTypeToDataTypeType(cz.lbenda.dataman.db.dialect.ColumnType columnType) {
    return DataTypeType.fromValue(columnType.name());
  }

  public static cz.lbenda.dataman.db.dialect.ColumnType dataTypeTypeToColumnType(DataTypeType dataType) {
    return cz.lbenda.dataman.db.dialect.ColumnType.valueOf(dataType.name());
  }

  public static class ForeignKey {
    private final String name; public final String getName() { return name; }
    private final TableDesc masterTable; public final TableDesc getMasterTable() { return masterTable; }
    private final ColumnDesc masterColumn; public final ColumnDesc getMasterColumn() { return masterColumn; }
    private final TableDesc slaveTable; public final TableDesc getSlaveTable() { return slaveTable; }
    private final ColumnDesc slaveColumn; public final ColumnDesc getSlaveColumn() { return slaveColumn; }
    private final String updateRule; public final String getUpdateRule() { return updateRule; }
    private final String deleteRule; public final String getDeleteRule() { return deleteRule; }

    public ForeignKey(@Nonnull String name, @Nonnull TableDesc masterTable, @Nonnull ColumnDesc masterColumn,
                      @Nonnull TableDesc slaveTable, @Nonnull ColumnDesc slaveColumn,
                      @Nonnull String updateRule , @Nonnull String deleteRule) {
      this.name = name;
      this.masterTable = masterTable;
      this.masterColumn = masterColumn;
      this.slaveTable = slaveTable;
      this.slaveColumn = slaveColumn;
      this.updateRule = updateRule;
      this.deleteRule = deleteRule;
    }
  }

  @FunctionalInterface
  private interface CatalogHolder {
    CatalogDesc getCatalog(String name);
  }
}
