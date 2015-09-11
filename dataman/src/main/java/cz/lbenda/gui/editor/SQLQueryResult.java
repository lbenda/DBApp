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
package cz.lbenda.gui.editor;

import javafx.collections.ObservableList;

import java.io.IOException;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 7.9.15.
 * Object which hold deteched data from SQL query */
public class SQLQueryResult {
  private String sql; public String getSql() { return sql; } public void setSql(String sql) { this.sql = sql; }
  private String msg; public String getMsg() { return msg; } public void setMsg(String msg) { this.msg = msg; }
  private String errorMsg; public String getErrorMsg() { return errorMsg; } public void setErrorMsg(String error) { this.errorMsg = error; }
  private SQLQueryRows sqlQueryRows; public SQLQueryRows getSqlQueryRows() { return sqlQueryRows; } public void setSqlQueryRows(SQLQueryRows sqlQueryRows) { this.sqlQueryRows = sqlQueryRows; }
  private Integer affectedRow; public Integer getAffectedRow() { return affectedRow; } public void setAffectedRow(Integer affectedRow) { this.affectedRow = affectedRow; }

  public static class SQLQueryRows {
    private SQLQueryMetaData metaData; public SQLQueryMetaData getMetaData() { return metaData; } public void setMetaData(SQLQueryMetaData metaData) { this.metaData = metaData; }
    private ObservableList<SQLQueryRow> rows; public ObservableList<SQLQueryRow> getRows() { return rows; } public void setRows(ObservableList<SQLQueryRow> rows) { this.rows = rows; }
  }

  /** Metadata which hold information about returned table */
  public static class SQLQueryMetaData {
    private List<SQLQueryColumn> columns; public List<SQLQueryColumn> getColumns() { return columns; } public void setColumns(List<SQLQueryColumn> columns) { this.columns = columns; } public void setColumns(SQLQueryColumn[] columns) { this.columns = Arrays.asList(columns); }
    /** Position of column */
    public int columnIdx(SQLQueryColumn column) { return columns.indexOf(column); }
    public int columnCount() { return columns.size(); }
  }

  /** Description of single column in returned Query */
  public static class SQLQueryColumn {
    private String catalog; public String getCatalog() { return catalog; }
    private String schema; public String getSchema() { return schema; }
    private String table; public String getTable() { return table; }
    private String name; public String getName() { return name; }
    private String label; public String getLabel() { return label; }
    private int columnType; public int getColumnType() { return columnType; }
    private String columnTypeName; public String getColumnTypeName() { return columnTypeName; }
    private int displaySize; public int getDisplaySize() { return displaySize; }
    private String javaClassName; public String getJavaClassName() { return javaClassName; }
    public SQLQueryColumn(String name, String label) {
      this.name = name;
      this.label = label;
    }
    /** Create SQL Query by data from result set metadata
     * @param mtd Metadata which hold information about column
     * @param idx Index of column
     * @throws IOException
     */
    public SQLQueryColumn(ResultSetMetaData mtd, int idx) throws SQLException {
      this.name = mtd.getColumnName(idx);
      this.label = mtd.getColumnLabel(idx);
      this.catalog = mtd.getCatalogName(idx);
      this.schema = mtd.getSchemaName(idx);
      this.table = mtd.getTableName(idx);
      this.displaySize = mtd.getColumnDisplaySize(idx);
      this.columnType = mtd.getColumnType(idx);
      this.columnTypeName = mtd.getColumnTypeName(idx);
      this.javaClassName = mtd.getColumnClassName(idx);
    }
  }

  public static class SQLQueryRow {
    private int[] PRIMES = {17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229, 233, 239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293, 307, 311, 313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, 383, 389, 397, 401, 409, 419, 421, 431, 433, 439, 443, 449, 457, 461, 463, 467, 479, 487, 491, 499, 503, 509, 521, 523, 541, 547, 557, 563, 569, 571, 577, 587, 593, 599, 601, 607, 613, 617, 619, 631, 641, 643, 647, 653, 659, 661, 673, 677, 683, 691, 701, 709, 719, 727, 733, 739, 743, 751, 757, 761, 769, 773, 787, 797, 809, 811, 821, 823, 827, 829, 839, 853, 857, 859, 863, 877, 881, 883, 887, 907, 911, 919, 929, 937, 941, 947, 953, 967, 971, 977, 983, 991, 997};
    private Object id; public Object getId() { return id; } public void setId(Object id) { this.id = id; }
    private Object[] values; public Object[] getValues() { return values; } public void setValues(Object[] values) { this.values = values; }
    @Override
    public boolean equals(Object o) {
      if (o == null) { return false; }
      if (!(o instanceof SQLQueryRow)) { return false; }

      SQLQueryRow sqr = (SQLQueryRow) o;
      if (id != null) { return id.equals(sqr.getId()); }
      else if (sqr.getId() != null) { return false; }

      if (values == null) { return sqr.getValues() == null; }
      else if (sqr.getValues() == null) { return false; }
      return Arrays.equals(values, sqr.getValues());
    }
    @Override
    public int hashCode() {
      if (id != null) { return id.hashCode(); }
      if (values == null) { return 0; }
      int result = 1;
      int primeI = 0;
      for (Object v : values) {
        if (primeI >= PRIMES.length) { primeI = 0; }
        result *= PRIMES[primeI];
        if (v != null) { result += v.hashCode(); }
      }
      return result;
    }
  }
}
