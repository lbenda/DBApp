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
package cz.lbenda.dataman.db.sql;

import cz.lbenda.dataman.db.DbConfig;
import cz.lbenda.rcp.action.AbstractAction;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;

import java.util.function.Consumer;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Action which run SQL command */
@ActionConfig(
    category = "/SQL/sql",
    id = "cz.lbenda.dataman.db.sql.SQLRunAllHandler",
    priority = 150,
    gui = @ActionGUIConfig(
      displayName = @Message(id="Run_all", msg="Run all"),
      displayTooltip = @Message(id="Run_all_tooltip", msg="Run sql commands which is selected or whole content of editor."),
      iconBase = "sqlRunAll.png"
    )
)
public class SQLRunAllHandler extends AbstractAction {

  private SQLEditorController sqlEditorController;
  private ObjectProperty<DbConfig> dbConfigProperty;
  /** Function which is call when something is write to console and should be show */
  private Consumer<SQLSExecutor> consoleShower;
  private ChangeListener<Boolean> connectionListener = (observableValue, oldValue, newValue) -> setEnable(newValue);

  public SQLRunAllHandler(ObjectProperty<DbConfig> dbConfigProperty, SQLEditorController sqlEditorController,
                          Consumer<SQLSExecutor> consoleShower) {
    this.sqlEditorController = sqlEditorController;
    this.dbConfigProperty = dbConfigProperty;
    this.consoleShower = consoleShower;
    dbConfigProperty.addListener((observable, oldValue, newValue) -> {
      if (oldValue != null) {
        oldValue.getConnectionProvider().connectedProperty().removeListener(connectionListener);
      }
      if (newValue == null) {
        setEnable(false);
      } else {
        setEnable(newValue.connectionProvider.isConnected());
        newValue.connectionProvider.connectedProperty().addListener(connectionListener);
      }
    });
  }

  @Override
  public void handle(ActionEvent e) {
    new SQLSExecutor(dbConfigProperty.getValue(), sqlEditorController, consoleShower)
        .execute(sqlEditorController.getExecutedText(false));
  }
}
