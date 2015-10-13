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

import cz.lbenda.common.AbstractHelper;
import cz.lbenda.rcp.action.AbstractSavable;
import cz.lbenda.dataman.schema.exconf.AuditType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

/** Main object which hold all information about database table structure
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/16/14. */
public class TableDesc extends AbstractSavable implements Comparable<TableDesc> {

  public enum TableType {
    NULL, TABLE, VIEW, SYSTEM_TABLE, INDEX, SYSTEM_INDEX, SYSTEM_TOAST_INDEX, SYSTEM_VIEW, UNDEFINED,
    FOREIGN_TABLE, SYSTEM_TOAST_TABLE, TEMPORARY_INDEX, TEMPORARY_SEQUENCE, TEMPORARY_TABLE,
    TEMPORARY_VIEW, TYPE, PROCEDURE, UDT, SEQUENCE, ALIAS ;
    public static TableType fromJDBC(String tt) {
      if (tt == null) { return UNDEFINED; }
      return TableType.valueOf(tt.replace(" ", "_"));
    }
  }

  private DbConfig dbConfig;  public final DbConfig getDbConfig() { return dbConfig; } public final void setDbConfig(DbConfig dbConfig) { this.dbConfig = dbConfig; }

  private final SchemaDesc schema; public final SchemaDesc getSchema() { return schema; }
  private final String name; public final String getName() { return name; }
  private TableType tableType; public final TableType getTableType() { return tableType; }
  private String comment;
  @SuppressWarnings("unused")
  public final String getComment() { return comment; }
  public final void setComment(String comment) { this.comment = comment; }

  private BooleanProperty hidden = new SimpleBooleanProperty(false);
  public boolean isHidden() { return hidden.get(); }
  @SuppressWarnings("unused")
  public void setHidden(boolean hidden) { this.hidden.set(hidden); }
  @SuppressWarnings("unused")
  public BooleanProperty hiddenProperty() { return this.hidden; }

  /** List of all foreign keys in table */
  private final List<DbStructureFactory.ForeignKey> foreignKeys = new ArrayList<>(); public final List<DbStructureFactory.ForeignKey> getForeignKeys() { return foreignKeys; }

  /** All rows, loaded new added and removed etc. */
  private SQLQueryRows rows; public SQLQueryRows getQueryRow() { return rows; }
  /** List of all columns in table */
  public final List<ColumnDesc> getColumns() { return rows.getMetaData().getColumns(); }
  /** All extension which extend the GUI feature of table */
  private final List<TableDescriptionExtension> extensions = new ArrayList<>(); public final List<TableDescriptionExtension> getExtensions() { return this.extensions; }
  /** All extension which is inform about the table is change. Mainly it's extension of another table */
  private final List<TableDescriptionExtension> reloadableExtension = new ArrayList<>(); public final List<TableDescriptionExtension> getReloadableExtension() { return reloadableExtension; }
  /** Audit configuration for this table */
  private AuditType audit = TableDescriptionExtension.NONE_AUDIT; public final AuditType getAudit() { return audit; } public final void setAudit(AuditType audit) { this.audit = audit; }

  /** Inform about dirty state of table */
  private BooleanProperty dirty = new SimpleBooleanProperty(false);
  public @Nonnull BooleanProperty dirtyProperty() { return dirty; }
  @SuppressWarnings("unused")
  public boolean isDirty() { return Boolean.TRUE.equals(dirty.getValue()); }

  /** Inform if data in table is already loaded */
  private ObjectProperty<Boolean> loaded = new SimpleObjectProperty<>(false);
  @SuppressWarnings("unused")
  public ObjectProperty<Boolean> loadedProperty() { return loaded; }
  public boolean isLoaded() { return Boolean.TRUE.equals(loaded.getValue()); }

  public TableDesc(SchemaDesc schema, String tableType, String name) {
    this.schema = schema;
    this.name = name;
    if (tableType != null) { this.tableType = TableType.fromJDBC(tableType); }
    rows = new SQLQueryRows();
    rows.getRows().addListener((ListChangeListener<RowDesc>) change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          change.getAddedSubList().forEach(row -> {
            if (row.getState() != RowDesc.RowDescState.LOADED) {
              dirty.setValue(true);
            }
            row.addListener((observable) -> {
              RowDesc currRow = (RowDesc) observable;
              if (currRow.getState() != RowDesc.RowDescState.LOADED) {
                dirty.setValue(true);
              } else {
                dirty.setValue(!getRows().stream().allMatch(tr -> tr.getState() == RowDesc.RowDescState.LOADED));
              }
            });
          });
        } else if (change.wasRemoved()) {
          if (!change.getRemoved().stream().allMatch(row -> row.getState() == RowDesc.RowDescState.LOADED)) {
            dirty.setValue(!getRows().stream().allMatch(row -> row.getState() == RowDesc.RowDescState.LOADED));
          }
        }
      }
    });
    loaded.addListener((observable, oldValue, newValue) -> {
      if (newValue) { register(); }
      else { unregister(); }
    });
  }

  public final void addForeignKey(DbStructureFactory.ForeignKey foreignKey) {
    this.foreignKeys.add(foreignKey);
  }

  /** Return list of all columns, which is in primary key */
  public final List<ColumnDesc> getPKColumns() {
    return rows.getMetaData().getPKColumns();
  }

  public final void addColumn(ColumnDesc column) {
    column.setPosition(getColumns().size() + 1);
    getColumns().add(column);
  }

  public final ColumnDesc getColumn(String columnName) {
    for (ColumnDesc result : getColumns()) {
      if (result.getName().equals(columnName)) { return result; }
    }
    return null;
  }

  /** Count of column in one row */
  public final int columnCount() {
    return getColumns().size();
  }

  /** Return list of extensions which are defined for one column
   * @param column column which extensions is requested
   * @return list of extensions (if extensions missing, then empty list is returned)
   */
  public final List<TableDescriptionExtension> getColumnExtensions(final ColumnDesc column) {
    List<TableDescriptionExtension> result = new ArrayList<>(2);
    result.addAll(getExtensions().stream().filter(tde -> tde.getColumns().contains(column)).collect(Collectors.toList()));
    return result;
  }

  @Override
  public final int compareTo(@Nonnull TableDesc other) {
    return AbstractHelper.compareArrayNull(new Comparable[]{schema, tableType, name},
        new Comparable[]{ other.getSchema(), other.getTableType(), other.getName() });
  }

  public ObservableList<RowDesc> getRows() { return rows.getRows(); }

  /** Method which add new row to table */
  public RowDesc addNewRowAction() {
    RowDesc row = RowDesc.createNewRow(rows.getMetaData());
    rows.getRows().add(row);
    return row;
  }

  /** Cancel changes in all rows */
  @SuppressWarnings("unused")
  public void cancelChangesAction() {
    for (Iterator<RowDesc> itt = getRows().iterator(); itt.hasNext(); ) {
      RowDesc rowDesc = itt.next();
      if (RowDesc.RowDescState.NEW.equals(rowDesc.getState())) { itt.remove(); }
      else if (!RowDesc.RowDescState.LOADED.equals(rowDesc.getState())) { rowDesc.cancelChanges(); }
    }
  }

  /** Reload all data from database - remove changes as part of */
  public void reloadRowsAction() {
    getRows().clear();
    if (dbConfig.getConnectionProvider() == null ||
        !dbConfig.getConnectionProvider().isConnected()) {
      ConnectionProvider.notConnectedDialog(dbConfig);
    }
    if (dbConfig.getConnectionProvider() != null &&
        dbConfig.getConnectionProvider().isConnected()) {
      dbConfig.getReader().readTableData(this, -1, -1).forEach(row -> getRows().add(row));
      this.getQueryRow().setSQL(String.format("select * from \"%s\".\"%s\"", getSchema().getName(), getName()));
      this.loaded.setValue(Boolean.TRUE);
    }
  }

  /** Save all changes to database */
  public void saveChangesAction() {
    getDbConfig().getDbRowManipulator().saveChanges(this);
    unregister();
  }

  /** Inform if table is editable. But event the table is editable can contains some uneditable column */
  public boolean isEditable() {
    return this.getTableType() == TableType.TABLE;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 41 * hash + (this.name != null ? this.name.hashCode() : 0);
    hash = 41 * hash + (this.schema != null ? this.schema.hashCode() : 0);
    hash = 41 * hash + (this.tableType != null ? this.tableType.hashCode() : 0);
    return hash;
  }

  @Override
  public @Nonnull String displayName() {
    return String.format("%s: %s.%s.%s", getDbConfig().getId(),
        schema.getCatalog().getName(), schema.getName(), name);
  }

  @Override
  public void save() {
    if (Boolean.TRUE.equals(dirtyProperty().getValue())) {
      this.saveChangesAction();
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) { return false; }
    if (getClass() != obj.getClass()) { return false; }
    final TableDesc other = (TableDesc) obj;
    if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) { return false; }
    if ((this.schema == null) ? (other.schema != null) : !this.schema.equals(other.schema)) { return false; }
    //noinspection SimplifiableIfStatement
    return this.tableType == null ? (other.tableType == null) : this.tableType.equals(other.tableType);
  }

  @Override
  public String toString() {
    return name;
  }
}
