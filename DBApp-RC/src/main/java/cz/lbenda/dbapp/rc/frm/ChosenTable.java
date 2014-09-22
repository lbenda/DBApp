/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.lbenda.dbapp.rc.frm;

import cz.lbenda.dbapp.rc.db.DbStructureReader.Column;
import cz.lbenda.dbapp.rc.db.TableDescription;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** This table is singleton which hold information about chosen table and chosen row in this table
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public final class ChosenTable {

  public interface ChosenTableListener {
    void tableChosen(TableDescription tableDescription);
  }

  public interface ChosenRowListener {
    void rowChosen(Map<Column, Object> selectedRowValues);
  }

  public interface RowUpdateListener {
    void rowUpdated(TableDescription td, Map<Column, Object> oldValues, Map<Column, Object> newValues);
  }

  private final static ChosenTable instance = new ChosenTable();

  private TableDescription tableDescription; public TableDescription getTableDescription() { return tableDescription; }
    private Map<Column, Object>selectedRowValues; public Map<Column, Object> getSelectedRowValues() { return selectedRowValues; }
  public final Set<ChosenTableListener> tableListeners = new HashSet<>();
  public final Set<ChosenRowListener> rowListeners = new HashSet<>();
  public final Set<RowUpdateListener> rowUpdateListeners = new HashSet<>();

  private ChosenTable() {}

  public static ChosenTable getInstance() {
    return instance;
  }

  public void setTableDescription(TableDescription tableDescription) {
    this.tableDescription = tableDescription;
    for (ChosenTableListener listener : tableListeners) {
      listener.tableChosen(tableDescription);
    }
  }

  public void setSelectedRowValues(Map<Column, Object>selectedRowValues) {
    this.selectedRowValues = selectedRowValues;
    for (ChosenRowListener listener : rowListeners) {
      listener.rowChosen(selectedRowValues);
    }
  }

  public void updateRowValues(TableDescription td, Map<Column, Object> oldValues,
          Map<Column, Object> newValues) {
    for (RowUpdateListener listener : rowUpdateListeners) {
      listener.rowUpdated(td, oldValues, newValues);
    }
  }

  public void addTableListener(ChosenTableListener listener) {
    tableListeners.add(listener);
  }

  public void removeTableListener(ChosenTableListener listener) {
    tableListeners.remove(listener);
  }

  public void addRowListener(ChosenRowListener listener) {
    rowListeners.add(listener);
  }

  public void removeRowListener(ChosenRowListener listener) {
    rowListeners.remove(listener);
  }

  public void addRowUpdateListener(RowUpdateListener listener) {
    rowUpdateListeners.add(listener);
  }

  public void removeRowUpdateListener(RowUpdateListener listener) {
    rowUpdateListeners.remove(listener);
  }
}
