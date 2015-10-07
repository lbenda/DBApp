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

import cz.lbenda.common.Constants;
import cz.lbenda.dataman.db.*;
import cz.lbenda.dataman.rc.ComboBoxItemTableCell;
import cz.lbenda.gui.tableView.*;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
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

  private final TableDesc tableDesc; public TableDesc getTableDesc() { return tableDesc; }
  private final SQLQueryRows sqlQueryRows; public SQLQueryRows getSqlQueryRows() { return sqlQueryRows; }
  private final Map<TableColumn<RowDesc, ?>, ColumnDesc> columnColumns = new WeakHashMap<>();

  public DataTableView(@Nonnull TableDesc tableDesc) {
    this(tableDesc, tableDesc.getQueryRow());
  }

  public DataTableView(@Nonnull SQLQueryRows sqlQueryRows) {
    this(null, sqlQueryRows);
  }

  private DataTableView(TableDesc tableDesc, @Nonnull SQLQueryRows sqlQueryRows) {
    super();
    this.tableDesc = tableDesc;
    this.sqlQueryRows = sqlQueryRows;
    this.filters().add(row -> RowDesc.RowDescState.REMOVED != row.getState());
    this.setEditable(tableDesc != null && TableDesc.TableType.TABLE.equals(tableDesc.getTableType()));
  }

  @SuppressWarnings("unchecked")
  public void setMetaData(SQLQueryMetaData metaData) {
    this.getColumns().clear();
    for (final ColumnDesc column : metaData.getColumns()) {
      FilterableTableColumn<RowDesc, Object> tc = new FilterableTableColumn<>(column.getName(), column.getLabel());
      tc.setPrefWidth(preferWidth(column));
      tc.setCellValueFactory(var1 -> var1.getValue().valueProperty(column));
      tc.setCellFactory(tableCellFactory(tc.getCellFactory(), tc, column));//tableColumn -> tableCellForColumn(column));
      this.columnColumns.put(tc, column);
      this.getColumns().add(tc);
    }
  }

  /** Return prefer width of column */
  private double preferWidth(ColumnDesc column) {
    final Canvas canvas = new Canvas(250, 250);
    GraphicsContext gc = canvas.getGraphicsContext2D();
    double nWidth = com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().computeStringWidth("N", gc.getFont()) * 0.9;
    switch (column.getDataType()) {
      case BIT: return 3 * nWidth;
      case BOOLEAN: return 10 * nWidth;
      case BLOB:
      case CLOB:
      case BYTE_ARRAY:
        return 30 * nWidth;
      case UUID:
        return 36 * nWidth;
      case TIME:
      case DATE:
        // return 10 * nWidth; non editable
        return 10 * nWidth;
      case TIMESTAMP:
        return 20 * nWidth;
      default:
        double prefSize = column.getSize() / Math.log(column.getSize());
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
      case BOOLEAN: return javafx.scene.control.cell.CheckBoxTableCell.forTableColumn(tc);
      case BYTE:
      case SHORT:
      case INTEGER:
      case LONG:
      case FLOAT:
      case DOUBLE:
      case DECIMAL: return (Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>>) TextFieldTableCell.forTableColumn(columnDesc.getStringConverter());
      case DATE: return (Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>>) (Object) DatePickerTableCell.forTableColumn();
      case TIME: return (Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>>) (Object) TimePickerTableCell.forTableColumn();
      case TIMESTAMP: return (Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>>) (Object) DateTimePickerTableCell.forTableColumn();
      case STRING: return (Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>>) (Object) TextAreaTableCell.forTableColumn(
          columnDesc.toString(), columnDesc.getSize() <= Constants.MIN_SIZE_FOR_TEXT_AREA);
      case BYTE_ARRAY:
      case BLOB: return (Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>>) (Object) BinaryDataTableCell.forTableColumn(false);
      case CLOB: return (Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>>) (Object) BinaryDataTableCell.forTableColumn(true);
      default:
        return (Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>>) TextFieldTableCell.forTableColumn(columnDesc.getStringConverter());
    }
  }

  @Override
  public void setRows(ObservableList<RowDesc> rows) {
    super.setRows(rows);
    InvalidationListener il = observable -> refilter();
    rows.forEach(row -> row.addListener(il));
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
