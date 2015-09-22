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

import cz.lbenda.dataman.rc.DbConfig;
import cz.lbenda.rcp.action.AbstractAction;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Action which save all changes in all tables */
@ActionConfig(
    category = "/Table/save",
    id = "cz.lbenda.dataman.db.handler.SaveAllTableHandler",
    priority = 5,
    gui = @ActionGUIConfig(
        iconBase = "table-saveAll.png",
        displayName = @Message(id="SaveAll_table", msg="Save all"),
        displayTooltip = @Message(id="SaveAll_table_tooltip", msg="Save all changes from current sessions")
    )
)
public class SaveAllTableHandler extends AbstractAction {

  private ObjectProperty<DbConfig> dbConfigProperty;
  private ChangeListener<Boolean> dirtyListener = (observableValue, oldValue, newValue) -> setEnable(newValue);

  public SaveAllTableHandler(ObjectProperty<DbConfig> dbConfigProperty) {
    this.dbConfigProperty = dbConfigProperty;
    dbConfigProperty.addListener((observableValue, oldValue, newValue) -> {
      if (oldValue != null && oldValue.getReader() != null && oldValue.getReader().getSavableRegistry() != null) {
        oldValue.getReader().getSavableRegistry().dirtyProperty().removeListener(dirtyListener);
      }
      if (newValue != null && newValue.getReader() != null && newValue.getReader().getSavableRegistry() != null) {
        setEnable(newValue.getReader().getSavableRegistry().dirtyProperty().getValue());
        newValue.getReader().getSavableRegistry().dirtyProperty().addListener(dirtyListener);
      } else { setEnable(false); }
    });
  }

  @Override
  public void handle(ActionEvent event) {
    DbConfig dbConfig = dbConfigProperty.getValue();
    if (dbConfig != null && dbConfig.getReader() != null) {
      dbConfig.getReader().close((Stage) ((Node) event.getSource()).getScene().getWindow());
    }
  }
}