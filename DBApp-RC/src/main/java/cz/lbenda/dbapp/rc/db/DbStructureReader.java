/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.rc.db;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Class for reading data structure from JDBC
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public class DbStructureReader {

  private static final Logger LOG = LoggerFactory.getLogger(DbStructureReader.class);

  private JDBCConfiguration jdbcConfiguration;
  private List<TableDescription> tableDescriptions = new ArrayList<>();
  private final Map<String, Map<String, Map<String, TableDescription>>> tableDescriptionsMap
          = new HashMap<>();

  private ComboPooledDataSource dataSource = null;

  private static final DbStructureReader instance = new DbStructureReader();

  public static final DbStructureReader getInstance() { return instance; }

  public void changeJDBCConfiguration(JDBCConfiguration jdbcConfiguration) {
    this.jdbcConfiguration = jdbcConfiguration;
    try {
      if (dataSource != null) { dataSource.close(); }
      dataSource = new ComboPooledDataSource();
      dataSource.setDriverClass(jdbcConfiguration.getDriverClass());
      dataSource.setJdbcUrl(jdbcConfiguration.getUrl());
      dataSource.setUser(jdbcConfiguration.getUsername());
      dataSource.setPassword(jdbcConfiguration.getPassword());
      dataSource.setMinPoolSize(1);
      dataSource.setMaxPoolSize(2);
      dataSource.setMaxStatements(2);
    } catch (Exception e) {
      LOG.error("Connection can't be create", e);
      throw new RuntimeException("Connection can't be create", e);
    }
  }

  /** Inheritance protection */
  private DbStructureReader() {};

  public final Connection getConnection() throws SQLException {
    return dataSource.getConnection();
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

  public final TableDescription getTableDescription(String tableName) {
    for (TableDescription td : getStructure()) {
      if (tableName.equals(td.getName())) { return td; }
    }
    return null;
  }

  public final List<TableDescription> getStructure() {
    if (tableDescriptions.isEmpty()) {
      tableDescriptions = generateStructure();
    }
    return tableDescriptions;
  }

  /** Method return data which are in joined table. The binding is defined by
   * <b>fk</b>.
   * @param fk foreign key which describe connection between tables
   * @param selectedRow all values of selected row
   * @param masterRow flag which inform if selected row is master (fk is exported) or slave (fk is imported)
   * @return resul set or null
   */
  public final List<Object[]> getJoinedRows(final ForeignKey fk, final Map<Column, Object> selectedRow,
          final boolean masterRow) {
    final Object fkValue;
    final String tbSchema;
    final String tbName;
    final String tbColumn;
    final TableDescription td;
    if (masterRow) {
      fkValue = selectedRow.get(fk.getMasterColumn());
      tbSchema = fk.getSlaveTable().getSchema();
      tbName = fk.getSlaveTable().getName();
      tbColumn = fk.getSlaveColumn().getName();
      td = fk.getSlaveTable();
    } else {
      fkValue = selectedRow.get(fk.getSlaveColumn());
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
          List<Object[]> result = new ArrayList<>();
          while (rs.next()) {
            Object[] row = new Object[td.getColumns().size()];
            for (int i = 0; i < td.getColumns().size(); i++) {
              row[i] = rs.getObject(i + 1);
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

  public TableDescription getTableDescription(String catalog, String schema, String table) {
    return tableDescriptionsMap.get(catalog).get(schema).get(table);
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
      String sql = String.format("insert into \"%s\" (%s) values (%s)", td.getName(), names, values);
      LOG.debug(sql);
      try (Connection conn = getConnection()) {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
          int i = 1;
          for (Column col : changedColumns) {
            ps.setObject(i, newValues.get(col));
            i++;
          }

          ps.execute();
          ResultSet rs = ps.getGeneratedKeys();
          while (rs.next()) { // FIXME : This isn't the best solution, because there can be more auto generated fields and no every must be PK
            Column col = td.getPKColumns().get(0);
            newValues.put(col, rs.getObject(1));
          }
          LOG.debug("New column was inserted");
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
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
      String sql = String.format("update \"%s\" set %s where %s", td.getName(), set, where);
      LOG.debug(sql);
      try (Connection conn = getConnection()) {
        try (PreparedStatement ps = conn.prepareCall(sql)) {
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
    try (Connection conn = getConnection();) {
      try (PreparedStatement ps = conn.prepareCall(sql);) {
        int i = 1;
        for (Column col : pks) {
          ps.setObject(i, rowValues.get(col));
          i++;
        }
        ps.execute();
        LOG.debug("Record was deleted.");
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void createMapFromList(List<TableDescription> list) {
    tableDescriptionsMap.clear();
    for (TableDescription td : list) {
      Map<String, Map<String, TableDescription>> catgMap = tableDescriptionsMap.get(td.getCatalog());
      if (catgMap == null) {
        catgMap = new HashMap<>();
        tableDescriptionsMap.put(td.getCatalog(), catgMap);
      }
      Map<String, TableDescription> schMap = catgMap.get(td.getSchema());
      if (schMap == null) {
        schMap = new HashMap<>();
        catgMap.put(td.getSchema(), schMap);
      }
      schMap.put(td.getName(), td);
    }
  }

  private List<TableDescription> generateStructure() {
    try (Connection conn = getConnection()) {
      DatabaseMetaData dmd = conn.getMetaData();

      List<TableDescription> result;
      try (ResultSet tabs = dmd.getTables(null, null, null, null)) {
        result = new ArrayList<>();
        while (tabs.next()) {
          TableDescription td = new TableDescription(tabs.getString("TABLE_CAT"),
                  tabs.getString("TABLE_SCHEM"), tabs.getString("TABLE_TYPE"),
                  tabs.getString("TABLE_NAME"));
          result.add(td);
        }
        java.util.Collections.sort(result);
        createMapFromList(result);
        generateStructureColumns(dmd);
        generatePKColumns(dmd, result);
        generateStrucutreForeignKeys(dmd, result);
      }
      return result;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void generateStructureColumns(DatabaseMetaData dmd) throws SQLException {
    try (ResultSet rsColumn  = dmd.getColumns(null, null, null, null)) {
      while (rsColumn.next()) {
        Column column = new Column(rsColumn.getString("COLUMN_NAME"), rsColumn.getInt("DATA_TYPE"),
                rsColumn.getInt("COLUMN_SIZE"), "YES".equals(rsColumn.getString("IS_NULLABLE")),
                "YES".equals(rsColumn.getString("IS_AUTOINCREMENT")),
                "YES".equals(rsColumn.getString("IS_GENERATEDCOLUMN")));
        getTableDescription(rsColumn.getString("TABLE_CAT"), rsColumn.getString("TABLE_SCHEM"),
                rsColumn.getString("TABLE_NAME")).addColumn(column);
      }
    }
  }

  private void generatePKColumns(DatabaseMetaData dmd, List<TableDescription> list) throws SQLException {
    for (TableDescription td : list) {
      try (ResultSet rsPk = dmd.getPrimaryKeys(td.getCatalog(), td.getSchema(), td.getName())) {
        while (rsPk.next()) {
          Column column = td.getColumn(rsPk.getString("COLUMN_NAME"));
          column.setPK(true);
        }
      }
    }
  }

  private void generateStrucutreForeignKeys(DatabaseMetaData dmd, List<TableDescription> list) throws SQLException {
    for (TableDescription td : list) {
      ResultSet rsEx = dmd.getExportedKeys(td.getCatalog(), td.getSchema(), td.getName());
      while (rsEx.next()) {
        TableDescription slaveTD = getTableDescription(rsEx.getString("FKTABLE_CAT"),
                rsEx.getString("FKTABLE_SCHEM"), rsEx.getString("FKTABLE_NAME"));
        ForeignKey fk = new ForeignKey(td, td.getColumn(rsEx.getString("PKCOLUMN_NAME")),
                slaveTD, slaveTD.getColumn(rsEx.getString("FKCOLUMN_NAME")));
        td.addForeignKey(fk);
        slaveTD.addForeignKey(fk);
      }
    }
  }

  public static class TableDescription implements Comparable<TableDescription> {
    private final String name; public final String getName() { return name; }
    private final String schema; public final String getSchema() { return schema; }
    private final String catalog; public final String getCatalog() { return catalog; }
    private final String tableType; public final String getTableType() { return tableType; }

    private final List<ForeignKey> foreignKeys = new ArrayList<>(); public final List<ForeignKey> getForeignKeys() { return foreignKeys; }
    private final List<Column> columns = new ArrayList<>(); public final List<Column> getColumns() { return columns; }

    public TableDescription(String catalog, String schema, String tableType, String name) {
      this.catalog = catalog;
      this.schema = schema;
      this.name = name;
      this.tableType = tableType;
    }

    public final void addForeignKey(ForeignKey foreignKey) {
      this.foreignKeys.add(foreignKey);
    }

    /** Return list of all columns, which is in primary key */
    public final List<Column> getPKColumns() {
      List<Column> result = new ArrayList<>();
      for (Column col : columns) {
        if (col.isPK()) { result.add(col); }
      }
      return result;
    }

    public final void addColumn(Column column) {
      column.setPosition(this.columns.size());
      this.columns.add(column);
    }

    public final Column getColumn(String columnName) {
      for (Column result : columns) {
        if (result.getName().equals(columnName)) { return result; }
      }
      return null;
    }

    public final String getColumnString(String colName, Map<Column, Object> rowValue) {
      Column col = getColumn(colName);
      if (col != null) { return col.getColumnString(rowValue); }
      return null;
    }

    @Override
    public final String toString() {
      return name;
    }

    @Override
    public final int compareTo(TableDescription other) {
      if (other == null) { throw new NullPointerException(); }
      if (!TableDescription.class.equals(other.getClass())) { throw new ClassCastException(); }
      if (this.catalog.equals(other.catalog)) {
        if (this.schema.equals(other.schema)) {
          if (this.tableType.equals(other.tableType)) {
            return this.name.compareTo(other.name);
          }
          return this.tableType.compareTo(other.tableType);
        }
        return this.schema.compareTo(other.schema);
      }
      return this.catalog.compareTo(other.catalog);
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 41 * hash + (this.name != null ? this.name.hashCode() : 0);
      hash = 41 * hash + (this.schema != null ? this.schema.hashCode() : 0);
      hash = 41 * hash + (this.catalog != null ? this.catalog.hashCode() : 0);
      hash = 41 * hash + (this.tableType != null ? this.tableType.hashCode() : 0);
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final TableDescription other = (TableDescription) obj;
      if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
        return false;
      }
      if ((this.schema == null) ? (other.schema != null) : !this.schema.equals(other.schema)) {
        return false;
      }
      if ((this.catalog == null) ? (other.catalog != null) : !this.catalog.equals(other.catalog)) {
        return false;
      }
      return this.tableType == null ? (other.tableType == null) : this.tableType.equals(other.tableType);
    }
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

  public static class Column {
    public enum ColumnType {
      STRING, INTEGER, DATE, DATE_TIME, OBJECT ;
    }

    private final String name; public final String getName() { return name; }
    private final int size; public final int getSize() { return size; }
    private final ColumnType dataType; public final ColumnType getDataType() { return dataType; }
    private final boolean nullable; public final boolean isNullable() { return nullable; }
    private int position; public final int getPosition() { return position; } public final void setPosition(int position) { this.position = position; }
    private boolean pk; public final boolean isPK() { return pk; } public final void setPK(boolean pk) { this.pk = pk; }
    private boolean autoincrement; public final boolean isAutoincrement() { return autoincrement; }
    private boolean generated; public final boolean isGenerated() { return generated; }

    public Column(final String name, final int dataType, final int size, final boolean nullable,
            final boolean autoincrement, final boolean generated) {
      this.name = name;
      this.size = size;
      this.nullable = nullable;
      this.autoincrement = autoincrement;
      this.generated = generated;

      switch (dataType) {
        case Types.TIMESTAMP : this.dataType = ColumnType.DATE_TIME; break;
        case Types.DATE : this.dataType = ColumnType.DATE; break;
        case Types.INTEGER : this.dataType = ColumnType.INTEGER; break;
        case Types.CHAR :
        case Types.VARCHAR : this.dataType = ColumnType.STRING; break;
        default : this.dataType = ColumnType.OBJECT;
      }
    }

    public String getColumnString(Map<Column, Object> values) {
      return String.valueOf(values.get(this)); // TODO inteligent convertor
    }

    @Override
    public String toString() { return pk ? "* " + name : name; }
  }
}
