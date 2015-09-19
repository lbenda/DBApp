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
package cz.lbenda.dataman.db.frm;

import cz.lbenda.dataman.db.*;
import cz.lbenda.dataman.rc.ComboBoxItemTableCell;
import cz.lbenda.rcp.tableView.DatePickerTableCell;
import cz.lbenda.rcp.tableView.DateTimePickerTableCell;
import cz.lbenda.rcp.tableView.FilterableTableColumn;
import cz.lbenda.rcp.tableView.FilterableTableView;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import javafx.util.StringConverter;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Stream;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 18.9.15.
 * Table view which show row desc
 */
public class DataTableView extends FilterableTableView<RowDesc> {

  private final TableDesc tableDesc;
  private final Map<TableColumn<RowDesc, ?>, ColumnDesc> columnColumns = new WeakHashMap<>();

  public TableDesc getTableDesc() {
    return tableDesc;
  }

  public DataTableView(TableDesc tableDesc) {
    super();
    this.tableDesc = tableDesc;
    this.filters().add(row -> RowDesc.RowDescState.REMOVED != row.getState());
    if (tableDesc != null) { this.setEditable(true); }
  }

  @SuppressWarnings("unchecked")
  public void setMetaData(SQLQueryMetaData metaData) {
    this.getColumns().clear();
    for (final ColumnDesc column : metaData.getColumns()) {
      FilterableTableColumn<RowDesc, Object> tc = new FilterableTableColumn<>(column.getName(), column.getLabel());
      tc.setPrefWidth(preferWidth(column));
      tc.setCellValueFactory(var1 -> var1.getValue().observableValueForColumn(column));
      tc.setCellFactory(tableCellFactory(tc.getCellFactory(), tc, column));//tableColumn -> tableCellForColumn(column));
      this.columnColumns.put(tc, column);
      this.getColumns().add(tc);
    }
  }

  /**
   * Return prefer width of column
   */
  private double preferWidth(ColumnDesc column) {
    final Canvas canvas = new Canvas(250, 250);
    GraphicsContext gc = canvas.getGraphicsContext2D();
    double nWidth = com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().computeStringWidth("N", gc.getFont()) * 0.9;
    switch (column.getDataType()) {
      case DATE:
        // return 10 * nWidth; non editable
        return 25 * nWidth;
      default:
        double prefSize = column.getDisplaySize() / Math.log(column.getDisplaySize());
        if (prefSize > 100) {
          prefSize = 100;
        } else if (prefSize < 10) {
          prefSize = 10;
        }
        return prefSize * nWidth;
    }
  }

  @SuppressWarnings("unchecked")
  private Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>> tableCellFactory(
      Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>> originalFactory,
      TableColumn tc, ColumnDesc columnDesc) {
    if (columnDesc.isAutoincrement() || Boolean.TRUE.equals(columnDesc.isGenerated())) {
      return originalFactory;
    }

    if (!columnDesc.getExtensions().isEmpty()) {
      for (TableDescriptionExtension tde : columnDesc.getExtensions()) {
        if (tde instanceof ComboBoxTDExtension) {
          return ComboBoxItemTableCell.forTableColumn((ComboBoxTDExtension) tde);
        }
      }
    }
    switch (columnDesc.getDataType()) {
      case BOOLEAN:
        return CheckBoxTableCell.forTableColumn(tc);
      /*case INTEGER:
        return (Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>>) (Object) TextFieldTableCell.forTableColumn(new IntegerStringConverter());*/
      case DATE:
        return (Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>>) (Object) DatePickerTableCell.forTableColumn();
      case TIME:
      case TIMESTAMP:
        return (Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>>) (Object) DateTimePickerTableCell.forTableColumn();
      default:
        return (Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>>) (Object) TextFieldTableCell.forTableColumn();
    }
  }

  @Override
  public void setRows(ObservableList<RowDesc> rows) {
    super.setRows(rows);
    InvalidationListener il = observable -> {
      refilter();
      // getItems().filtered(REMOVE_PREDICATE);
      // getItems().remove(observable);
    };
    rows.forEach(row -> row.addListener(il));
    /*
    rows.addListener((ListChangeListener<RowDesc>) change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          getItems().addAll(change.getAddedSubList());
          change.getAddedSubList().forEach(row -> row.addListener(il));
        }
        if (change.wasRemoved()) {
          getItems().removeAll(change.getRemoved());
        }
      }
    });
    getItems().addAll(rows);
    */
  }

  @Override
  public <T> Stream<T> valuesForColumn(@Nonnull TableColumn<RowDesc, ?> tableColumn) {
    //noinspection unchecked
    return getItems().stream().map(row -> (T) row.getColumnValue(columnColumns.get(tableColumn)));
  }

  @Override
  public <T> StringConverter<T> stringConverter(TableColumn<RowDesc, ?> tableColumn) {
    //noinspection unchecked
    return columnColumns.get(tableColumn).getStringConverter();
  }

  @Override
  public <T> T valueForColumn(@Nonnull RowDesc row, @Nonnull TableColumn<RowDesc, ?> tableColumn) {
    //noinspection unchecked
    return (T) row.getColumnValue(columnColumns.get(tableColumn));
  }
}
