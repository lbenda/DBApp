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
package cz.lbenda.dataman.db.handler;

import cz.lbenda.dataman.db.RowDesc;
import cz.lbenda.dataman.db.frm.DataTableFrmController;
import cz.lbenda.rcp.action.AbstractAction;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.TableView;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * remove rows from table */
@ActionConfig(
    category = "/SQL/table",
    id = "cz.lbenda.dataman.db.handler.RemoveRowsHandler",
    priority = 200,
    gui = @ActionGUIConfig(
        iconBase = "edit-table-delete-row.png",
        displayName = @Message(id="Remove_rows", msg="Delete"),
        displayTooltip = @Message(id="Remove_rows_tooltip", msg="Delete all selected rows")
    )
)
public class RemoveRowsHandler extends AbstractAction {

  private ObjectProperty<DataTableFrmController.DataTableView> tableViewObjectProperty;

  public RemoveRowsHandler(ObjectProperty<DataTableFrmController.DataTableView> tableViewObjectProperty) {
    this.tableViewObjectProperty = tableViewObjectProperty;
    setEnable(tableViewObjectProperty.getValue() != null && tableViewObjectProperty.getValue().isEditable());
    tableViewObjectProperty.addListener((observableValue, oldValue, newValue) -> {
      setEnable(newValue != null && newValue.isEditable());
    });
  }

  @Override
  public void handle(ActionEvent e) {
    TableView tableView = tableViewObjectProperty.getValue();
    if (tableView != null && tableView.isEditable()) {
      tableView.getSelectionModel().getSelectedItems().forEach(item -> {
        if (item instanceof RowDesc) { ((RowDesc) item).setState(RowDesc.RowDescState.REMOVED); }
        else { tableView.getItems().remove(item); }
      });
    }
  }
}