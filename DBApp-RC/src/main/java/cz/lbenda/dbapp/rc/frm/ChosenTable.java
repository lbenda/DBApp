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
package cz.lbenda.dbapp.rc.frm;

import cz.lbenda.dbapp.rc.SessionConfiguration;
import cz.lbenda.dbapp.rc.db.Column;
import cz.lbenda.dbapp.rc.db.TableDescription;

import java.util.HashMap;
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
    void rowChosen(TableDescription td, RowNode.Row selectedRowValues);
  }

  public interface RowUpdateListener {
    void rowUpdated(TableDescription td, Map<Column, Object> oldValues, Map<Column, Object> newValues);
  }

  public interface ConfigurationUpdateListener {
    void configurationChanged();
  }

  public interface SessionChangeListener {
    void sessionConfigurationChanged(SessionConfiguration sc);
  }

  private final static ChosenTable instance = new ChosenTable();

  public final Set<ChosenTableListener> tableListeners = new HashSet<>();
  public final Set<ChosenRowListener> rowListeners = new HashSet<>();
  public final Set<RowUpdateListener> rowUpdateListeners = new HashSet<>();
  public final Set<ConfigurationUpdateListener> configurationUpdateListeners = new HashSet<>();
  private final Set<SessionChangeListener> sessionChangeListeners = new HashSet<>();

  private ChosenTable() {}

  public static ChosenTable getInstance() {
    return instance;
  }

  public void setTableDescription(TableDescription tableDescription) {
    for (ChosenTableListener listener : tableListeners) {
      listener.tableChosen(tableDescription);
    }
  }

  public void setSelectedRowValues(RowNode.Row row) {
    for (ChosenRowListener listener : rowListeners) {
      listener.rowChosen(row.getTableDescription(), row);
    }
  }

  public void updateRowValues(TableDescription td, Map<Column, Object> oldValues,
          Map<Column, Object> newValues) {
    for (RowUpdateListener listener : rowUpdateListeners) {
      listener.rowUpdated(td, oldValues, newValues);
    }
  }

  public void configurationUpdated() {
    for (ConfigurationUpdateListener l : configurationUpdateListeners) {
      l.configurationChanged();
    }
  }

  public void changeSessionConfiguration(SessionConfiguration sc) {
    for (SessionChangeListener l : sessionChangeListeners) {
      l.sessionConfigurationChanged(sc);
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

  public void addConfigurationUpdateListener(ConfigurationUpdateListener l) { configurationUpdateListeners.add(l); }
  public void removeConfigurationUpdateListener(ConfigurationUpdateListener l) { configurationUpdateListeners.remove(l); }

  public void addSessionChangeListener(SessionChangeListener l) { this.sessionChangeListeners.add(l); }
  public void removeSessionChangeListener(SessionChangeListener l) { this.sessionChangeListeners.remove(l); }
}
