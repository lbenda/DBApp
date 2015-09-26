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

import cz.lbenda.common.StringConverters;
import cz.lbenda.common.BinaryData;
import javafx.util.StringConverter;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Date;

/** Description of column in database
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/26/14.
 */
public class ColumnDesc {

  public enum ColumnType {
    SHORT(Short.class, StringConverters.SHORT_CONVERTER),
    BYTE(Byte.class, StringConverters.BYTE_CONVERTER),
    INTEGER(Integer.class, StringConverters.INT_CONVERTER),
    LONG(Long.class, StringConverters.LONG_CONVERTER),
    BYTEARRAY(Byte[].class, StringConverters.BINARYDATA_CONVERTER),

    FLOAT(Float.class, StringConverters.FLOAT_CONVERTER),
    DOUBLE(Double.class, StringConverters.DOUBLE_CONVERTER),
    DECIMAL(BigDecimal.class, StringConverters.DECIMAL_CONVERTER),

    BOOLEAN(Boolean.class, StringConverters.BOOLEAN_CONVERTER),
    STRING(String.class, StringConverters.STRING_CONVERTER),
    CLOB(BinaryData.class, StringConverters.BINARYDATA_CONVERTER),

    DATE(Date.class, StringConverters.SQL_DATE_CONVERTER),
    TIMESTAMP(Timestamp.class, StringConverters.SQL_TIMESTAMP_CONVERTER),
    TIME(Time.class, StringConverters.SQL_TIME_CONVERTER),

    BLOB(BinaryData.class, StringConverters.BINARYDATA_CONVERTER),

    OBJECT(Object.class, StringConverters.OBJECT_CONVERTER),
    ;

    private final Class clazz;
    private final StringConverter converter;
    ColumnType(Class clazz, StringConverter converter) {
      this.clazz = clazz;
      this.converter = converter;
    }
    @SuppressWarnings("unused")
    public Class getJavaClass() { return clazz; }
    public StringConverter getConverter() { return converter; }
  }

  private TableDesc tableDescription;
  @SuppressWarnings("unused")
  public final TableDesc getTableDescription() { return tableDescription; }
  private final String catalog; public String getCatalog() { return catalog; }
  private final String schema; public String getSchema() { return schema; }
  private final String table; public String getTable() { return table; }
  private final String name; public final String getName() { return name; }
  private final int displaySize; public int getDisplaySize() { return displaySize; }
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
  public ColumnDesc(ResultSetMetaData mtd, int position) throws SQLException {
    this.position = position;
    this.name = mtd.getColumnName(position);
    this.label = mtd.getColumnLabel(position);
    this.catalog = mtd.getCatalogName(position);
    this.schema = mtd.getSchemaName(position);
    this.table = mtd.getTableName(position);
    this.displaySize = mtd.getColumnDisplaySize(position);
    this.dataType = sqlToColumnType(mtd.getColumnType(position));
    this.columnTypeName = mtd.getColumnTypeName(position);
    this.javaClassName = mtd.getColumnClassName(position);
    System.out.println(mtd.getColumnType(position) + ": " + dataType + " - " + javaClassName);
    this.nullable = ResultSetMetaData.columnNullable == mtd.isNullable(position) ? Boolean.TRUE :
        (ResultSetMetaData.columnNoNulls == mtd.isNullable(position) ? Boolean.FALSE : null);

    this.autoincrement = mtd.isAutoIncrement(position);
    generated = this.autoincrement ? Boolean.TRUE : null;
  }

  public ColumnDesc(final TableDesc td, final String name, final String label, final int dataType, final int size,
                    final boolean nullable, final boolean autoincrement, final boolean generated) {
    this.tableDescription = td;
    this.position = -1;
    this.catalog = td.getCatalog();
    this.schema = td.getSchema();
    this.table = td.getName();
    this.name = name;
    this.displaySize = size;
    this.nullable = nullable;
    this.autoincrement = autoincrement;
    this.generated = generated;
    this.dataType = sqlToColumnType(dataType);
    this.label = label;
  }

  private ColumnType sqlToColumnType(int dataType) {
    switch (dataType) {
      case Types.TIMESTAMP : return ColumnType.TIMESTAMP;
      case Types.TIME : return ColumnType.TIME;
      case Types.DATE : return ColumnType.DATE;
      case Types.INTEGER : return ColumnType.INTEGER;
      case Types.BIGINT : return ColumnType.LONG;
      case Types.TINYINT : return ColumnType.BYTE;
      case Types.SMALLINT : return ColumnType.SHORT;
      case Types.FLOAT :
      case Types.REAL : return ColumnType.FLOAT;
      case Types.NUMERIC :
      case Types.DECIMAL : return ColumnType.DECIMAL;
      case Types.DOUBLE : return ColumnType.DOUBLE;
      case Types.CHAR :
      case Types.VARCHAR : return ColumnType.STRING;
      case Types.BOOLEAN : return ColumnType.BOOLEAN;
      case Types.VARBINARY:
      case Types.BINARY : return ColumnType.BYTEARRAY;
      case Types.CLOB : return ColumnType.CLOB;
      case Types.BLOB : return ColumnType.BLOB;
      case Types.OTHER :
      default :
        System.out.println("Unresolved dataType: " + dataType);
        return ColumnType.OBJECT;
    }
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ColumnDesc)) return false;

    ColumnDesc column = (ColumnDesc) o;

    return autoincrement == column.autoincrement && generated == column.generated && nullable == column.nullable
        && pk == column.pk && position == column.position && displaySize == column.displaySize
        && dataType == column.dataType && !(name != null ? !name.equals(column.name) : column.name != null);
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + displaySize;
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
