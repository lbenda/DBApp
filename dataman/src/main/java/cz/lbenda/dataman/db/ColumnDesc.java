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

import cz.lbenda.dataman.db.dialect.ColumnType;
import cz.lbenda.dataman.db.dialect.SQLDialect;
import javafx.util.StringConverter;

import javax.annotation.Nonnull;
import java.sql.*;
import java.util.*;

/** Description of column in database
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/26/14.
 */
public class ColumnDesc {

  private TableDesc tableDescription;
  @SuppressWarnings("unused")
  public final TableDesc getTableDescription() { return tableDescription; }
  private final String catalog; public String getCatalog() { return catalog; }
  private final String schema; public String getSchema() { return schema; }
  private final String table; public String getTable() { return table; }
  private final String name; public final String getName() { return name; }
  private final int size; public int getSize() { return size; }
  private final ColumnType dataType; public final ColumnType getDataType() { return dataType; }
  private final Boolean nullable; @SuppressWarnings("unused") public final Boolean isNullable() { return nullable; }
  private int position; public final int getPosition() { return position; } public final void setPosition(int position) { this.position = position; }
  private boolean pk; public final boolean isPK() { return pk; }
  private final boolean autoincrement; public final boolean isAutoincrement() { return autoincrement; }
  private final Boolean generated; public final Boolean isGenerated() { return generated; }
  private final String label; public final String getLabel() { return label; }
  private String columnTypeName;
  @SuppressWarnings("unused")
  public String getColumnTypeName() { return columnTypeName; }
  private String javaClassName;
  @SuppressWarnings("unused")
  public String getJavaClassName() { return javaClassName; }

  /** Create SQL Query by data from result set metadata
   * @param mtd Metadata which hold information about column
   * @param position Index of column
   * @throws SQLException
   */
  public ColumnDesc(ResultSetMetaData mtd, int position, SQLDialect dialect) throws SQLException {
    this.position = position;
    this.name = mtd.getColumnName(position);
    this.label = mtd.getColumnLabel(position);
    this.catalog = mtd.getCatalogName(position);
    this.schema = mtd.getSchemaName(position);
    this.table = mtd.getTableName(position);
    this.size = mtd.getColumnDisplaySize(position);
    this.columnTypeName = mtd.getColumnTypeName(position);
    this.dataType = dialect.columnTypeFromSQL(mtd.getColumnType(position), this.columnTypeName, this.size);
    this.javaClassName = mtd.getColumnClassName(position);
    this.nullable = ResultSetMetaData.columnNullable == mtd.isNullable(position) ? Boolean.TRUE :
        (ResultSetMetaData.columnNoNulls == mtd.isNullable(position) ? Boolean.FALSE : null);

    this.autoincrement = mtd.isAutoIncrement(position);
    generated = this.autoincrement ? Boolean.TRUE : null;
  }

  @SuppressWarnings("unchecked")
  private <T> T confValue(String propertyName, ResultSet rs) throws SQLException {
    if (propertyName == null) { return null; }
    return (T) rs.getObject(propertyName);
  }
  private boolean confBool(String propertyName, ResultSet rs) throws SQLException {
    return propertyName != null && "YES".equals(rs.getString(propertyName));
  }

  /** Create column description and fill it with data which is from result set */
  public ColumnDesc(final TableDesc td, ResultSet rs, SQLDialect dialect) throws SQLException {
    this.tableDescription = td;
    this.position = -1;
    this.catalog = td.getCatalog();
    this.schema = td.getSchema();
    this.table = td.getName();
    this.name = confValue(dialect.columnName(), rs);
    this.label = confValue(dialect.columnRemarks(), rs);
    this.size = confValue(dialect.columnSize(), rs);
    this.columnTypeName = confValue(dialect.columnTypeName(), rs);
    this.nullable = confBool(dialect.columnNullable(), rs);
    this.autoincrement = confBool(dialect.columnAutoIncrement(), rs);
    this.generated = confBool(dialect.columnGenerated(), rs);
    this.dataType = dialect.columnTypeFromSQL(confValue(dialect.columnDateType(), rs), columnTypeName, size);

    /*
    if ("DATAMAN".equals(this.schema)) {
      System.out.println(this.name
          + ", SQLType: " + confValue(dialect.columnDateType(), rs)
          + ", Column type name: " + this.columnTypeName
          + ", Identified type: " + this.dataType);
    }
    */
  }

  public ColumnDesc(final TableDesc td, final String name, final String label, final int dataType, final String columnTypeName,
                    final int size, final boolean nullable, final boolean autoincrement, final boolean generated,
                    final SQLDialect dialect) {
    this.tableDescription = td;
    this.position = -1;
    this.catalog = td.getCatalog();
    this.schema = td.getSchema();
    this.table = td.getName();
    this.name = name;
    this.size = size;
    this.nullable = nullable;
    this.autoincrement = autoincrement;
    this.generated = generated;
    this.dataType = dialect.columnTypeFromSQL(dataType, columnTypeName, size);
    this.label = label;
  }

  /** Set primary key and add this column to set of primary column in to metadata */
  public final void setPK(boolean pk) {
    this.pk = pk;
    if (tableDescription != null && tableDescription.getQueryRow() != null) {
      if (pk) { tableDescription.getQueryRow().getMetaData().getPKColumns().add(this); }
      else { tableDescription.getQueryRow().getMetaData().getPKColumns().remove(this); }
    }
  }

  /** Return string converter which is commonly used with date type which is hold by field in this column
   * @return String converter */
  public StringConverter getStringConverter() {
    return dataType.getConverter();
  }

  /** Return extensions which are defined for this column in tableDescriptor
   * @return if no extension is defined return empty list */
  @Nonnull
  public List<TableDescriptionExtension> getExtensions() {
    if (tableDescription == null) { return Collections.emptyList(); }
    return tableDescription.getColumnExtensions(this);
  }

  /** Inform if this column is editable. */
  public boolean isEditable() {
    return this.tableDescription != null && this.tableDescription.isEditable() && !this.isGenerated();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ColumnDesc)) return false;

    ColumnDesc column = (ColumnDesc) o;

    return autoincrement == column.autoincrement && generated == column.generated && nullable == column.nullable
        && pk == column.pk && position == column.position && size == column.size
        && dataType == column.dataType && !(name != null ? !name.equals(column.name) : column.name != null);
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
  public String toString() {
    return (getLabel() != null
        ? getLabel() + " (" + getSchema() + "." + getTable() + "." + getName() + ")"
        : getSchema() + "." + getTable() + "." + getName()) + (pk ? " - PK " : "");
  }
}
