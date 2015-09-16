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
import cz.lbenda.rcp.DatePickerTableCell;
import cz.lbenda.rcp.DateTimePickerTableCell;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;

import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;

import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import javafx.util.converter.IntegerStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 10.9.15.
 * Controller for showing data in table */
public class DataTableFrmController {

  private static final Logger LOG = LoggerFactory.getLogger(DataTableFrmController.class);
  private DataTableView tableView;

  private static Map<TableDesc, DataTableFrmController> controllers = new HashMap<>();

  public DataTableFrmController(TableDesc td) {
    tableView = new DataTableView(td);
    td.reloadRowsAction();
    this.tableView.setMetaData(td.getQueryRow().getMetaData());
    this.tableView.setRows(td.getRows());
  }

  public DataTableFrmController(SQLQueryResult sqlQueryResult) {
    tableView = new DataTableView(null);
    this.tableView.setMetaData(sqlQueryResult.getSqlQueryRows().getMetaData());
    this.tableView.setRows(sqlQueryResult.getSqlQueryRows().getRows());
  }

  /** Return parent node which hold the controller */
  public DataTableView getTabView() {
    return tableView;
  }

  public static class DataTableView extends TableView<RowDesc> {
    private final TableDesc tableDesc; public TableDesc getTableDesc() { return tableDesc; }
    public DataTableView(TableDesc tableDesc) {
      this.tableDesc = tableDesc;
      if (tableDesc != null) {
        this.setEditable(true);
      }
    }

    private ObservableList<RowDesc> rows;
    @SuppressWarnings("unchecked")
    public void setMetaData(SQLQueryMetaData metaData) {
      this.getColumns().clear();
      for (final ColumnDesc column : metaData.getColumns()) {
        TableColumn<RowDesc, Object> tc = new TableColumn<>();

        Label colHeaderTextField = new Label(column.getName());
        if (column.getLabel() != null) {
          colHeaderTextField.setTooltip(new Tooltip(column.getLabel()));
        }
        tc.setGraphic(colHeaderTextField);
        tc.setPrefWidth(preferWidth(column));

        tc.setCellValueFactory(var1 -> var1.getValue().observableValueForColumn(column));
        tc.setCellFactory(tableCellFactory(tc.getCellFactory(), tc, column));//tableColumn -> tableCellForColumn(column));
        this.getColumns().add(tc);
      }
      this.setHeaderNodesEventHandler();
    }

    /** Return prefered width of column */
    private double preferWidth(ColumnDesc column) {
      final Canvas canvas = new Canvas(250,250);
      GraphicsContext gc = canvas.getGraphicsContext2D();
      double nWidth = com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().computeStringWidth("N", gc.getFont()) * 0.9;
      switch (column.getDataType()) {
        case DATE:
          // return 10 * nWidth; non editable
          return 25 * nWidth;
        default :
          double prefSize = column.getDisplaySize() / Math.log(column.getDisplaySize());
          if (prefSize > 100) { prefSize = 100; }
          else if (prefSize < 10) { prefSize = 10; }
          return prefSize * nWidth;
      }
    }

    @SuppressWarnings("unchecked")
    private Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>> tableCellFactory(
        Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>> originalFactory,
        TableColumn tc, ColumnDesc columnDesc) {
      if (columnDesc.isAutoincrement() || Boolean.TRUE.equals(columnDesc.isGenerated())) { return originalFactory; }

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
        case INTEGER:
          return (Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>>) (Object) TextFieldTableCell.forTableColumn(new IntegerStringConverter());
        case DATE:
          return (Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>>) (Object) DatePickerTableCell.forTableColumn();
        case TIME:
        case TIMESTAMP:
          return (Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>>) (Object) DateTimePickerTableCell.forTableColumn();
        default:
          return (Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>>) (Object) TextFieldTableCell.forTableColumn();
      }
    }

    private static Predicate<RowDesc> REMOVE_PREDICATE = row -> row.getState() != RowDesc.RowDescState.REMOVED;

    public void setFilterPredicate(Predicate<RowDesc> predicate) {
      FilteredList<RowDesc> fl = (FilteredList<RowDesc>) getItems();
      fl.setPredicate(predicate);
    }

    public void setRows(ObservableList<RowDesc> rows) {
      FilteredList<RowDesc> filteredList = new FilteredList<>(rows);
      InvalidationListener il = observable -> filteredList.setPredicate(REMOVE_PREDICATE);
      rows.forEach(row -> row.addListener(il));
      filteredList.setPredicate(row -> row.getState() != RowDesc.RowDescState.REMOVED); // Filter removed rows
      super.setItems(filteredList);
    }
    private void setHeaderNodesEventHandler() {
      int i = 0;
      for (final Node headerNode : lookupAll(".column-header")) {
        LOG.info("Header node found");
        // ((AbstractFilterableTableColumn<E, ?>) getColumns().get(i)).setHeaderNodeEventHandler(headerNode);
        i++;
      }
    }
  }
}
