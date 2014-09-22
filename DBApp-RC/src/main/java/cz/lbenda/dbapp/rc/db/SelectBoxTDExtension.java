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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** When the table is show then on defined column is select box
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/16/14.
 */
public class SelectBoxTDExtension implements TableDescriptionExtension {

  private static final Logger LOG = LoggerFactory.getLogger(SelectBoxTDExtension.class);

  private final TableDescription td; public final TableDescription getTableDescription() { return td; }
  /** Name of column which is substitued by select box */
  private String columnName; public final String getColumnName() { return columnName; } public final void setColumnName(final String columnName) { this.columnName = columnName; }
  /** SQL which return three fields with identifier which is write to column, showed value and tool tipe. */
  private String tableOfKeySQL; public final String getTableOfKeySQL() { return tableOfKeySQL; } public final void setTableOfKeySQL(final String tableOfKeySQL) { this.tableOfKeySQL = tableOfKeySQL; }

  private String columnValue; public final String getColumnValue() { return columnValue; } public final void setColumnValue(final String columnValue) { this.columnValue = columnValue; }
  private String columnChoice; public final String getcColumnChoice () { return columnChoice; } public final void setColumnChoice(final String columnChoice) { this.columnChoice = columnChoice; }
  private String columnTooltip; public final String getColumnTooltip() { return columnTooltip; } public final void setColumnTooltip(final String columnTooltip) { this.columnTooltip = columnTooltip; }

  private final List<Object> values = new ArrayList<>(); public final List<Object> getValues() { return values; }
  private final List<String> choices = new ArrayList<>(); public final List<String> getChoices() { return choices; }
  private final List<String> tooltips = new ArrayList<>(); public final List<String> getTooltips() { return tooltips; }

  public SelectBoxTDExtension(TableDescription td, String columnName, String tableOfKeySQL) {
    this.td = td;
    this.columnName = columnName;
    this.tableOfKeySQL = tableOfKeySQL;
  }

  /** Return the extended column */
  public DbStructureReader.Column getColumn() {
    return this.td.getColumn(this.columnName);
  }

  /** This method read data of selectbox
   * @param connection connection to database from which is data read (the connection isn't closed).
   * @throws java.sql.SQLException exception when data from conneciton is readed
   */
  public final void loadSelectBoxData(final Connection connection) throws SQLException {
    LOG.trace("load data to select box");
    try (Statement stm = connection.createStatement()) {
      try (ResultSet rs = stm.executeQuery(tableOfKeySQL)) {
        values.clear(); choices.clear(); tooltips.clear();
        while (rs.next()) {
          final Object value = columnValue == null ? rs.getObject(1) : rs.getObject(columnValue);
          final String choice = columnChoice == null ? rs.getString(2) : rs.getString(columnChoice);
          final String tooltip = columnTooltip == null ? rs.getString(3) : rs.getString(columnTooltip);

          values.add(value);
          choices.add(choice);
          tooltips.add(tooltip);
        }
      }
    }
  }

  @Override
  public void tableWasChanged(TableDescription td, TableAction action) {
    switch (action) {
      case UPDATE :
      case DELETE :
      case INSERT :
        try (Connection connection = DbStructureReader.getInstance().getConnection()) {
          loadSelectBoxData(connection);
        } catch (SQLException e) {
          LOG.error(String.format("The selctbox extension '%s.%s.%s' wasn't reloaded.",
              td.getSchema(), td.getName(), this.columnName), e);
        } break;
    }
  }

  @Override
  public final Element storeToElement() {
    Element res = new Element("selectBox");
    res.setAttribute("column", columnName).setAttribute("tableOfKeySQL", tableOfKeySQL)
        .setAttribute("column_value", columnValue).setAttribute("column_choice", columnChoice)
        .setAttribute("column_tooltip", columnTooltip);
    return res;
  }
}
