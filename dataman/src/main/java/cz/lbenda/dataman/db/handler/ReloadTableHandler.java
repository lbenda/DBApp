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

import cz.lbenda.dataman.db.frm.DataTableFrmController;
import cz.lbenda.rcp.action.AbstractAction;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Action which reload single table */
@ActionConfig(
    category = "/SQL/table",
    id = "cz.lbenda.dataman.db.handler.ReloadTableHandler",
    priority = 10,
    gui = @ActionGUIConfig(
        iconBase = "table-reload.png",
        displayName = @Message(id="Reload_table", msg="Reload"),
        displayTooltip = @Message(id="Reload_table_tooltip", msg="Reload current table")
    )
)
public class ReloadTableHandler extends AbstractAction {

  private ObjectProperty<DataTableFrmController.DataTableView> tableViewObjectProperty;

  public ReloadTableHandler(ObjectProperty<DataTableFrmController.DataTableView> tableViewObjectProperty) {
    this.tableViewObjectProperty = tableViewObjectProperty;
    setEnable(tableViewObjectProperty.getValue() != null && tableViewObjectProperty.getValue().isEditable());
    tableViewObjectProperty.addListener((observableValue, oldValue, newValue) -> {
      setEnable(newValue != null);
    });
  }

  @Override
  public void handle(ActionEvent e) {
    DataTableFrmController.DataTableView tableView = tableViewObjectProperty.getValue();
    if (tableView != null && tableView.isEditable()) { tableView.getTableDesc().reloadRowsAction(); }
  }
}