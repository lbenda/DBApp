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

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 12.9.15.
 * Row from table (not from custom SQL query */
public class TableRow implements SQLRow {

  private final TableDesc tableDescription; public final TableDesc getTableDescription() { return tableDescription; }
  private final Object[] rowValues; public final Object[] getRowValues() { return rowValues; }

  public TableRow(TableDesc td) {
    this.tableDescription = td;
    this.rowValues = new Object[td.columnCount()];
  }

  public TableRow(TableDesc td, Object[] rowValues) {
    this.tableDescription = td;
    this.rowValues = rowValues;
  }

  public final Object getValue(ColumnDesc column) {
    return rowValues[column.getPosition()];
  }
  public final Object getValue(int position) {
    return rowValues[position];
  }
  public final void setValue(ColumnDesc column, Object value) {
    rowValues[column.getPosition()] = value;
  }
  public final void setValue(int position, Object value) {
    rowValues[position] = value;
  }

  public final Object getId() {
    java.util.List<ColumnDesc> pks = tableDescription.getPKColumns();
    if (pks.isEmpty()) { return "Empty"; }
    return rowValues[pks.get(0).getPosition()];
  }
}
