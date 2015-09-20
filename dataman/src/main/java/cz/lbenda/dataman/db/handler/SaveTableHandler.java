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

import cz.lbenda.dataman.db.frm.DataTableView;
import cz.lbenda.rcp.action.AbstractAction;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Action which save single table */
@ActionConfig(
    category = "/SQL/table",
    id = "cz.lbenda.dataman.db.handler.SaveTableHandler",
    priority = 10,
    gui = @ActionGUIConfig(
        iconBase = "table-save.png",
        displayName = @Message(id="Save_table", msg="Save"),
        displayTooltip = @Message(id="Save_table_tooltip", msg="Save all changes into database")
    )
)
public class SaveTableHandler extends AbstractAction {

  private ObjectProperty<DataTableView> tableViewObjectProperty;
  private ChangeListener<Boolean> dirtyListener = (observableValue, oldValue, newValue) -> setEnable(newValue);

  public SaveTableHandler(ObjectProperty<DataTableView> tableViewObjectProperty) {
    this.tableViewObjectProperty = tableViewObjectProperty;
    setEnable(tableViewObjectProperty.getValue() != null && tableViewObjectProperty.getValue().isEditable());
    tableViewObjectProperty.addListener((observableValue, oldValue, newValue) -> {
      if (oldValue != null && oldValue.getTableDesc() != null) {
        oldValue.getTableDesc().dirtyProperty().removeListener(dirtyListener);
      }
      if (newValue != null && newValue.getTableDesc() != null) {
        setEnable(newValue.getTableDesc().dirtyProperty().getValue());
            newValue.getTableDesc().dirtyProperty().addListener(dirtyListener);
      } else { setEnable(false); }
    });
  }

  @Override
  public void handle(ActionEvent e) {
    DataTableView tableView = tableViewObjectProperty.getValue();
    if (tableView != null && tableView.isEditable()) { tableView.getTableDesc().saveChangesAction(); }
  }
}