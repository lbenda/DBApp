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

import cz.lbenda.dataman.db.ColumnDesc;
import cz.lbenda.dataman.db.RowDesc;
import cz.lbenda.rcp.localization.Message;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import org.controlsfx.control.PropertySheet;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 23.9.15.
 * Frame for editing single row value */
public class RowEditorFrmController {

  @Message
  public static final String WINDOW_TITLE = "Row value editor";

  private PropertySheet sheet = new PropertySheet();
  private ObjectProperty<DataTableView> tableViewObjectProperty;
  private ChangeListener<RowDesc> changeRowListener = (observableValue, oldValue, newValue) -> setRowValue(newValue);

  public RowEditorFrmController(ObjectProperty<DataTableView> tableViewObjectProperty) {
    this.tableViewObjectProperty = tableViewObjectProperty;
    tableViewObjectProperty.addListener((observable, oldValue, newValue) -> {
      if (oldValue != null) {
        oldValue.getSelectionModel().selectedItemProperty().removeListener(changeRowListener);
      }
      if (newValue != null) {
        setRowValue(newValue.getSelectionModel().getSelectedItem());
        newValue.getSelectionModel().selectedItemProperty().addListener(changeRowListener);
      }
    });
  }

  /** Return node which hold whole view */
  public Node getPane() { return sheet; }

  public void setRowValue(RowDesc row) {
    sheet.getItems().clear();
    for (ColumnDesc columnDesc : tableViewObjectProperty.getValue().getSqlQueryRows().getMetaData().getColumns()) {
      RowPropertyItem item = new RowPropertyItem(columnDesc, row, columnDesc.isEditable());
      sheet.getItems().add(item);
    }
  }
}
