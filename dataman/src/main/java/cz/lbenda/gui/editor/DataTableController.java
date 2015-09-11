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
package cz.lbenda.gui.editor;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;

import cz.lbenda.gui.editor.SQLQueryResult.SQLQueryRow;
import cz.lbenda.gui.editor.SQLQueryResult.SQLQueryColumn;
import cz.lbenda.gui.editor.SQLQueryResult.SQLQueryMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 10.9.15.
 * Controller for showing data in table */
public class DataTableController {

  private static final Logger LOG = LoggerFactory.getLogger(DataTableController.class);
  private DataTableView tableView;

  private SQLQueryResult sqlQueryResult; public SQLQueryResult getSqlQueryResult() { return sqlQueryResult; }
  public void setSqlQueryResult(SQLQueryResult sqlQueryResult) {
    this.sqlQueryResult = sqlQueryResult;
    this.tableView.setMetaData(sqlQueryResult.getSqlQueryRows().getMetaData());
    this.tableView.setRows(sqlQueryResult.getSqlQueryRows().getRows());
  }

  public DataTableController() {
    tableView = new DataTableView();
  }

  /** Return parent node which hold the controller */
  public Node getNode() {
    return tableView;
  }

  private static class DataTableView extends TableView<SQLQueryRow> {
    private SQLQueryMetaData metaData;
    private ObservableList<SQLQueryRow> rows;
    @SuppressWarnings("unchecked")
    public void setMetaData(SQLQueryMetaData metaData) {
      this.metaData = metaData;
      this.getColumns().clear();

      final Canvas canvas = new Canvas(250,250);
      GraphicsContext gc = canvas.getGraphicsContext2D();

      for (final SQLQueryColumn column : metaData.getColumns()) {
        TableColumn<SQLQueryRow, String> tc = new TableColumn<>();
        // tc.setText(column.getLabel() != null ? column.getLabel() : column.getName());
        tc.setText("");
        // TextField colHeaderTextField = new TextField(column.getName());
        // tc.setGraphic(colHeaderTextField);
        Label colHeaderTextField = new Label(column.getName());
        tc.setGraphic(colHeaderTextField);

        double width = com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().computeStringWidth("N", gc.getFont()) * 0.9;
        // float height = com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().getFontMetrics(gc.getFont()).getLineHeight();

        tc.setMaxWidth(column.getDisplaySize() * width);
        tc.setMinWidth(1 * width);
        double prefSize = column.getDisplaySize() / Math.log(column.getDisplaySize());
        if (prefSize > 100) { prefSize = 100; }
        if (Date.class.getName().equals(column.getJavaClassName())) { prefSize = 10; }
        tc.setPrefWidth(prefSize * width);

        // tc.setCellFactory();
        tc.setCellValueFactory(var1 -> {
          int columnIdx = metaData.columnIdx(column);
          Object o = var1.getValue().getValues()[columnIdx];
          return new SimpleStringProperty(o == null ? "" : String.valueOf(o));
        });
        this.getColumns().add(tc);
      }
      this.setHeaderNodesEventHandler();
    }
    /*
    private Callback<TableColumn<SQLQueryRow, SQLQueryColumn>,
            TableCell<SQLQueryRow, SQLQueryColumn>> createCellFactory() {
      return new Callback<TableColumn<SQLQueryRow, SQLQueryColumn>,
          TableCell<SQLQueryRow, SQLQueryColumn>>() {
        @Override
        public TableCell<SQLQueryRow, SQLQueryColumn> call(TableColumn<SQLQueryRow, SQLQueryColumn> var1) {
          TableCell<SQLQueryRow, SQLQueryColumn> result = new TableCell<>();
          var1.getValue().
          return null;
        }
      };
    }
    */
    public void setRows(ObservableList<SQLQueryResult.SQLQueryRow> rows) {
      super.setItems(rows);
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
