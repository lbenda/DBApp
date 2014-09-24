/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.rc.db;

// import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;
import cz.lbenda.dbapp.rc.SessionConfiguration;
import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/** Class for reading data structure from JDBC
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public class DbStructureReader {

  public interface DbStructureReaderSessionChangeListener {
    void sessionConfigurationChanged(DbStructureReader reader, SessionConfiguration sc);
  }

  private static final Logger LOG = LoggerFactory.getLogger(DbStructureReader.class);

  private SessionConfiguration sessionConfiguration;
  private List<TableDescription> tableDescriptions = new ArrayList<>();
  private final Map<String, Map<String, Map<String, TableDescription>>> tableDescriptionsMap
          = new HashMap<>();
  private final List<DbStructureReaderSessionChangeListener> listeners = new ArrayList<>();

  private DataSource dataSource = null;

  private static DbStructureReader instance = new DbStructureReader();

  public static DbStructureReader getInstance() { return instance; }


  public final void setSessionConfiguration(SessionConfiguration sessionConfiguration) {
    this.sessionConfiguration = sessionConfiguration;
    createDataSource();
    for (DbStructureReaderSessionChangeListener l : listeners) {
      l.sessionConfigurationChanged(this, sessionConfiguration);
    }
  }
  @SuppressWarnings("unused")
  public final SessionConfiguration getSessionConfiguration() { return sessionConfiguration; }

  /** Inheritance protection and protection before custome instance is created */
  private DbStructureReader() {}

  /** Inform if the reader is prepared for read data - the session configuration exist */
  public final boolean isPrepared() { return sessionConfiguration != null; }

  private void createDataSource() throws IllegalStateException {
    if (!isPrepared()) {
      throw new IllegalStateException("The DBStructureReader isn't yet prepared for create dataSource. Please check isPrepared() properties");
    }
    try {
      if (dataSource != null) { DataSources.destroy(dataSource); }
      dataSource = DataSources.pooledDataSource(new DBAppDataSource(sessionConfiguration));
    } catch (Exception e) {
      LOG.error("DataSource can't be create", e);
      throw new RuntimeException("DataSource can't be create", e);
    }
  }

  public Connection getConnection() {
    if (dataSource == null) { createDataSource(); }
    try {
      return dataSource.getConnection();
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

  @SuppressWarnings("unused")
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
          td.sqlWasFired(TableDescriptionExtension.TableAction.INSERT);
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
      try (PreparedStatement ps = conn.prepareCall(sql)) {
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

  public void addDbStructureReaderSessionChangeListener(DbStructureReaderSessionChangeListener l) {
    this.listeners.add(l);
  }

  @SuppressWarnings("unused")
  public void removeDbStructureReaderSessionChangeListener(DbStructureReaderSessionChangeListener l) {
    this.listeners.remove(l);
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
      STRING, INTEGER, DATE, DATE_TIME, OBJECT
    }

    private final String name; public final String getName() { return name; }
    private final int size; @SuppressWarnings("unused") public final int getSize() { return size; }
    private final ColumnType dataType; public final ColumnType getDataType() { return dataType; }
    private final boolean nullable; @SuppressWarnings("unused") public final boolean isNullable() { return nullable; }
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
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Column)) return false;

      Column column = (Column) o;

      if (autoincrement != column.autoincrement) return false;
      if (generated != column.generated) return false;
      if (nullable != column.nullable) return false;
      if (pk != column.pk) return false;
      if (position != column.position) return false;
      if (size != column.size) return false;
      if (dataType != column.dataType) return false;
      return !(name != null ? !name.equals(column.name) : column.name != null);

    }

    @Override
    public int hashCode() {
      int result = name != null ? name.hashCode() : 0;
      result = 31 * result + size;
      result = 31 * result + (dataType != null ? dataType.hashCode() : 0);
      result = 31 * result + (nullable ? 1 : 0);
      result = 31 * result + position;
      result = 31 * result + (pk ? 1 : 0);
      result = 31 * result + (autoincrement ? 1 : 0);
      result = 31 * result + (generated ? 1 : 0);
      return result;
    }

    @Override
    public String toString() { return pk ? "* " + name : name; }
  }
}
