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

import com.mchange.v2.c3p0.DataSources;
import cz.lbenda.dbapp.rc.SessionConfiguration;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class for reading data structure from JDBC
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public class DbStructureReader {

  private static final Logger LOG = LoggerFactory.getLogger(DbStructureReader.class);

  private SessionConfiguration sessionConfiguration;
  private List<TableDescription> tableDescriptions = new ArrayList<>();

  private DataSource dataSource = null;

  public final void setSessionConfiguration(SessionConfiguration sessionConfiguration) {
    this.sessionConfiguration = sessionConfiguration;
    createDataSource();
  }
  @SuppressWarnings("unused")
  public final SessionConfiguration getSessionConfiguration() { return sessionConfiguration; }

  public DbStructureReader() {}

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

  public void generateStructure() {
    try (Connection conn = getConnection()) {
      DatabaseMetaData dmd = conn.getMetaData();

      // List<TableDescription> result;
      try (ResultSet tabs = dmd.getTables(null, null, null, null)) {
        // result = new ArrayList<>();
        while (tabs.next()) {
          TableDescription td = this.sessionConfiguration.getOrCreateTableDescription(tabs.getString("TABLE_CAT"),
              tabs.getString("TABLE_SCHEM"), tabs.getString("TABLE_NAME"));
          td.setTableType(tabs.getString("TABLE_TYPE"));
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
    try (ResultSet rsColumn  = dmd.getColumns(null, null, null, null)) {
      while (rsColumn.next()) {
        TableDescription td = sessionConfiguration.getTableDescription(rsColumn.getString("TABLE_CAT"), rsColumn.getString("TABLE_SCHEM"),
                rsColumn.getString("TABLE_NAME"));
        Column column = new Column(td, rsColumn.getString("COLUMN_NAME"), rsColumn.getInt("DATA_TYPE"),
                rsColumn.getInt("COLUMN_SIZE"), "YES".equals(rsColumn.getString("IS_NULLABLE")),
                "YES".equals(rsColumn.getString("IS_AUTOINCREMENT")),
                "YES".equals(rsColumn.getString("IS_GENERATEDCOLUMN")));
        td.addColumn(column);
      }
    }
  }

  private void generatePKColumns(DatabaseMetaData dmd) throws SQLException {
    for (TableDescription td : sessionConfiguration.getTableDescriptions()) {
      try (ResultSet rsPk = dmd.getPrimaryKeys(td.getCatalog(), td.getSchema(), td.getName())) {
        while (rsPk.next()) {
          Column column = td.getColumn(rsPk.getString("COLUMN_NAME"));
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
    for (TableDescription td : sessionConfiguration.getTableDescriptions()) {
      ResultSet rsEx = dmd.getExportedKeys(td.getCatalog(), td.getSchema(), td.getName());
      while (rsEx.next()) {
        TableDescription slaveTD = sessionConfiguration.getTableDescription(rsEx.getString("FKTABLE_CAT"),
                rsEx.getString("FKTABLE_SCHEM"), rsEx.getString("FKTABLE_NAME"));
        ForeignKey fk = new ForeignKey(td, td.getColumn(rsEx.getString("PKCOLUMN_NAME")),
                slaveTD, slaveTD.getColumn(rsEx.getString("FKCOLUMN_NAME")));
        td.addForeignKey(fk);
        slaveTD.addForeignKey(fk);
      }
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
}
