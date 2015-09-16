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

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.reactfx.collection.ListChange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Metadata which hold information about returned table */
public class SQLQueryMetaData {
  private ObservableList<ColumnDesc> columns = FXCollections.observableArrayList(); public ObservableList<ColumnDesc> getColumns() { return columns; }
  public void setColumns(ColumnDesc[] columns) { this.columns.addAll(columns); }
  /** Position of column */
  public int columnIdx(ColumnDesc column) { return columns.indexOf(column); }
  public int columnCount() { return columns.size(); }
  public List<ColumnDesc> pks = new ArrayList<>(); public final List<ColumnDesc> getPKColumns() { return pks; }

  public SQLQueryMetaData() {
    columns.addListener((ListChangeListener<ColumnDesc>) change -> {
      while (change.next()) {
        if (change.wasAdded()) { change.getAddedSubList().stream()
            .filter(column -> column != null && Boolean.TRUE.equals(column.isPK()))
            .forEach(pks::add); }
        if (change.wasRemoved()) { change.getRemoved().stream()
            .filter(pks::contains)
            .forEach(pks::remove); }
      }
    });
  }
}
