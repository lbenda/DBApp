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

import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Description of column in database
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/26/14.
 */
public class Column {

  public enum ColumnType {
    BOOLEAN(Boolean.class), STRING(String.class), INTEGER(Integer.class), DATE(Date.class), DATE_TIME(Date.class), OBJECT(Object.class);
    private final Class clazz;
    private ColumnType(Class clazz) { this.clazz = clazz; }
    public Class getDataType() { return clazz; }
  }

  private final TableDescription tableDescription; public final TableDescription getTableDescription() { return tableDescription; }
  private final String name; public final String getName() { return name; }
  private final int size; @SuppressWarnings("unused") public final int getSize() { return size; }
  private final ColumnType dataType; public final ColumnType getDataType() { return dataType; }
  private final boolean nullable; @SuppressWarnings("unused") public final boolean isNullable() { return nullable; }
  private int position; public final int getPosition() { return position; } public final void setPosition(int position) { this.position = position; }
  private boolean pk; public final boolean isPK() { return pk; } public final void setPK(boolean pk) { this.pk = pk; }
  private boolean autoincrement; public final boolean isAutoincrement() { return autoincrement; }
  private boolean generated; public final boolean isGenerated() { return generated; }

  public Column(final TableDescription td, final String name, final int dataType, final int size, final boolean nullable,
                final boolean autoincrement, final boolean generated) {
    this.tableDescription = td;
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
      case Types.BOOLEAN : this.dataType = ColumnType.BOOLEAN; break;
      default : this.dataType = ColumnType.OBJECT;
    }
  }

  public List<TableDescriptionExtension> getExtensions() {
    return tableDescription.getColumnExtensions(this);
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
