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
import cz.lbenda.dataman.rc.DbConfig;
import cz.lbenda.rcp.action.AbstractSavable;
import cz.lbenda.dataman.schema.exconf.AuditType;
import javafx.collections.ObservableList;

import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/** Main object which hold all information about database table structure
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/16/14.
 */
public class TableDesc extends AbstractSavable implements Comparable<TableDesc> {

  public enum TableType {
    NULL, TABLE, VIEW, SYSTEM_TABLE, INDEX, SYSTEM_INDEX, SYSTEM_TOAST_INDEX, SYSTEM_VIEW, UNDEFINED,
    FOREIGN_TABLE, SYSTEM_TOAST_TABLE, TEMPORARY_INDEX, TEMPORARY_SEQUENCE, TEMPORARY_TABLE,
    TEMPORARY_VIEW, TYPE, PROCEDURE, UDT, SEQUENCE ;
    public static TableType fromJDBC(String tt) {
      if (tt == null) { return UNDEFINED; }
      return TableType.valueOf(tt.replace(" ", "_"));
    }
  }

  private DbConfig dbConfig;  public final DbConfig getDbConfig() { return dbConfig; } public final void setDbConfig(DbConfig dbConfig) { this.dbConfig = dbConfig; }

  private final String name; public final String getName() { return name; }
  private final String schema; public final String getSchema() { return schema; }
  private final String catalog; public final String getCatalog() { return catalog; }
  private TableType tableType; public final TableType getTableType() { return tableType; } public final void setTableType(TableType tableType) { this.tableType = tableType; }
  private String comment; public final String getComment() { return comment; } public final void setComment(String comment) { this.comment = comment; }

  /** List of all foreign keys in table */
  private final List<DbStructureReader.ForeignKey> foreignKeys = new ArrayList<>(); public final List<DbStructureReader.ForeignKey> getForeignKeys() { return foreignKeys; }

  /** All rows, loaded new added and removed etc. */
  private SQLQueryRows rows = new SQLQueryRows(); public SQLQueryRows getQueryRow() { return rows; }
  /** List of all columns in table */
  public final List<ColumnDesc> getColumns() { return rows.getMetaData().getColumns(); }
  /** All extension which extend the GUI feature of table */
  private final List<TableDescriptionExtension> extensions = new ArrayList<>(); public final List<TableDescriptionExtension> getExtensions() { return this.extensions; }
  /** All extension which is inform about the table is change. Mainly it's extension of another table */
  private final List<TableDescriptionExtension> reloadableExtension = new ArrayList<>(); public final List<TableDescriptionExtension> getReloadableExtension() { return reloadableExtension; }
  /** Audit configuration for this table */
  private AuditType audit = TableDescriptionExtension.NONE_AUDIT; public final AuditType getAudit() { return audit; } public final void setAudit(AuditType audit) { this.audit = audit; }

  private PropertyChangeSupport pch = new PropertyChangeSupport(this);

  public TableDesc(String catalog, String schema, String tableType, String name) {
    this.catalog = catalog;
    this.schema = schema;
    this.name = name;
    if (tableType != null) { this.tableType = TableType.valueOf(tableType); }
    rows = new SQLQueryRows();
  }

  public final void addForeignKey(DbStructureReader.ForeignKey foreignKey) {
    this.foreignKeys.add(foreignKey);
  }

  /** Return list of all columns, which is in primary key */
  public final List<ColumnDesc> getPKColumns() {
    return rows.getMetaData().getPKColumns();
  }

  public final void addColumn(ColumnDesc column) {
    column.setPosition(getColumns().size());
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

  /** Return string representation of value which is given in row
   * @param colName Name of column which value is returned
   * @param rowValue Value of row
   */
  public final String getColumnString(String colName, Map<ColumnDesc, Object> rowValue) {
    ColumnDesc col = getColumn(colName);
    if (col != null) { return col.getColumnString(rowValue); }
    return null;
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
  public final int compareTo(TableDesc other) {
    if (other == null) { throw new NullPointerException(); }
    return AbstractHelper.compareArrayNull(new Comparable[]{catalog, schema, tableType, name},
        new Comparable[]{other.getCatalog(), other.getSchema(), other.getTableType(), other.getName()});
  }

  public ObservableList<RowDesc> getRows() { return rows.getRows(); }

  /** Method which add new row to table */
  public RowDesc addNewRowAction() {
    RowDesc row = RowDesc.createNewRow(rows.getMetaData());
    rows.getRows().add(row);
    return row;
  }

  /** Remove rows on given position */
  public void removeRowsAction(int[] poz) {
    for (int po : poz) { rows.getRows().get(po).setState(RowDesc.RowDescState.REMOVED); }
  }

  /** Cancel changes in all rows */
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
    dbConfig.getReader().readTableDate(this, -1, -1).forEach(values -> {
      RowDesc row = new RowDesc(null, values, RowDesc.RowDescState.LOADED);
      getRows().add(row);
    });
  }

  /** Save all changes to database */
  public void saveChangesAction() {
    getDbConfig().getReader().deleteRows(this);
    getDbConfig().getReader().updateRows(this);
    getDbConfig().getReader().insertRows(this);
    unregister();
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
  protected String findDisplayName() {
    return String.format("%s: %s.%s.%s", getDbConfig().getId(),
        getCatalog(), getSchema(), getName());
  }

  @Override
  protected void handleSave() throws IOException {
    this.saveChangesAction();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) { return false; }
    if (getClass() != obj.getClass()) { return false; }
    final TableDesc other = (TableDesc) obj;
    if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) { return false; }
    if ((this.schema == null) ? (other.schema != null) : !this.schema.equals(other.schema)) { return false; }
    if ((this.catalog == null) ? (other.catalog != null) : !this.catalog.equals(other.catalog)) { return false; }
    return this.tableType == null ? (other.tableType == null) : this.tableType.equals(other.tableType);
  }

  @Override
  public String toString() {
    if (getName() != null) { return getName(); }
    return super.toString();
  }
}
