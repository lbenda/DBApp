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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
* Created by Lukas Benda <lbenda @ lbenda.cz> on 9/16/14.
*/
public class TableDescription implements Comparable<TableDescription> {
  private final String name; public final String getName() { return name; }
  private final String schema; public final String getSchema() { return schema; }
  private final String catalog; public final String getCatalog() { return catalog; }
  private final String tableType; public final String getTableType() { return tableType; }

  private final List<DbStructureReader.ForeignKey> foreignKeys = new ArrayList<>(); public final List<DbStructureReader.ForeignKey> getForeignKeys() { return foreignKeys; }
  private final List<DbStructureReader.Column> columns = new ArrayList<>(); public final List<DbStructureReader.Column> getColumns() { return columns; }
  /** All extension which extend the GUI fetchure of table */
  private final List<TableDescriptionExtension> extensions = new ArrayList<>(); public final List<TableDescriptionExtension> getExtensions() { return this.extensions; }
  /** All extension which is inform about the table is change. Mainly it's extension of another table */
  private final List<TableDescriptionExtension> reloadableExtension = new ArrayList<>(); public final List<TableDescriptionExtension> getReloadableExtension() { return reloadableExtension; }

  public TableDescription(String catalog, String schema, String tableType, String name) {
    this.catalog = catalog;
    this.schema = schema;
    this.name = name;
    this.tableType = tableType;
  }

  public final void addForeignKey(DbStructureReader.ForeignKey foreignKey) {
    this.foreignKeys.add(foreignKey);
  }

  /** Return list of all columns, which is in primary key */
  public final List<DbStructureReader.Column> getPKColumns() {
    List<DbStructureReader.Column> result = new ArrayList<>();
    for (DbStructureReader.Column col : columns) {
      if (col.isPK()) { result.add(col); }
    }
    return result;
  }

  public final void addColumn(DbStructureReader.Column column) {
    column.setPosition(this.columns.size());
    this.columns.add(column);
  }

  public final DbStructureReader.Column getColumn(String columnName) {
    for (DbStructureReader.Column result : columns) {
      if (result.getName().equals(columnName)) { return result; }
    }
    return null;
  }

  public final String getColumnString(String colName, Map<DbStructureReader.Column, Object> rowValue) {
    DbStructureReader.Column col = getColumn(colName);
    if (col != null) { return col.getColumnString(rowValue); }
    return null;
  }

  /** This method registred when somebody execute any change SQL action on the table. */
  public final void sqlWasFired(TableDescriptionExtension.TableAction tableAction) {
    for (TableDescriptionExtension tde : this.reloadableExtension) {
      tde.tableWasChanged(this, tableAction);
    }
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
