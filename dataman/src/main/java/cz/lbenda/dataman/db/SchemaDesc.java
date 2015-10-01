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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 30.9.15.
 * Representation of database schema */
@SuppressWarnings("unused")
public class SchemaDesc implements Comparable<SchemaDesc> {

  private CatalogDesc catalog; public CatalogDesc getCatalog() { return catalog; } public void setCatalog(CatalogDesc catalog) { this.catalog = catalog; }
  private String name; public String getName() { return name; } public void setName(String name) { this.name = name; }

  private BooleanProperty hidden = new SimpleBooleanProperty(false);
  public boolean isHidden() { return hidden.get(); }
  public void setHidden(boolean hidden) { this.hidden.set(hidden); }
  public BooleanProperty hiddenProperty() { return this.hidden; }

  private ObservableList<TableDesc> tables = FXCollections.observableArrayList();
  public ObservableList<TableDesc> getTables() { return tables; }

  /** Return table description by name */
  public TableDesc getTable(@Nonnull String name) {
    List<TableDesc> tds = tables.stream().filter(table -> name.equals(table.getName())).collect(Collectors.toList());
    return tds.size() == 0 ? null : tds.get(0);
  }

  public SchemaDesc() {}
  public SchemaDesc(CatalogDesc catalog, String name) {
    this.catalog = catalog;
    this.name = name;
  }

  @Override
  public int compareTo(@Nonnull SchemaDesc schemaDesc) {
    if (getClass().equals(schemaDesc.getClass())) {
      throw new ClassCastException("Input parameter isn't class: " + schemaDesc.getClass());
    }
    int result = getCatalog().compareTo(schemaDesc.getCatalog());
    if (result == 0) { result = name.compareTo(schemaDesc.getName()); }
    return result;
  }

  /** Return all table types which is in schema */
  public final List<TableDesc.TableType> allTableTypes() {
    return tables.stream().map(TableDesc::getTableType).distinct().collect(Collectors.toList());
  }

  /** Return all table types which is shown - at least one table with this type is not hide */
  public final List<TableDesc.TableType> shownTableTypes() {
    return tables.stream().filter(TableDesc::isHidden).map(TableDesc::getTableType).collect(Collectors.toList());
  }

  /** Return tables with given type */
  public final List<TableDesc> tablesByType(TableDesc.TableType tableType) {
    return tables.stream().filter(table -> table.getTableType().equals(tableType)).collect(Collectors.toList());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SchemaDesc)) return false;

    SchemaDesc that = (SchemaDesc) o;

    return !(catalog != null ? !catalog.equals(that.catalog) : that.catalog != null)
        && !(name != null ? !name.equals(that.name) : that.name != null)
        && !(hidden != null ? !hidden.equals(that.hidden) : that.hidden != null);
  }

  @Override
  public int hashCode() {
    int result = catalog != null ? catalog.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (hidden != null ? hidden.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return name;
  }
}
