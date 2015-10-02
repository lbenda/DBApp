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

import cz.lbenda.dataman.db.DbConfig;
import cz.lbenda.rcp.action.AbstractAction;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Action which run SQL command */
@ActionConfig(
    category = "/DbConfig/connect",
    id = "cz.lbenda.dataman.db.handler.ConnectDatabaseHandler",
    priority = 10,
    showInMenuButton = false,
    gui = {
        @ActionGUIConfig(
            iconBase = "db-disconnect.png",
            displayName = @Message(id="Connect_database", msg="Connect"),
            displayTooltip = @Message(id="Connect_databse_tooltip", msg="Connect selected database")
        ),
        @ActionGUIConfig(
            iconBase = "db-connect.png",
            displayName = @Message(id="Disconnect_database", msg="Disconnect"),
            displayTooltip = @Message(id="Disconnect_databse_tooltip", msg="Disconnect selected database")
        )}
)
public class ConnectDatabaseHandler extends AbstractAction {

  @SuppressWarnings("unused")
  private static Logger LOG = LoggerFactory.getLogger(ConnectDatabaseHandler.class);

  /** The holder to which is set session configuration values */
  private ObjectProperty<DbConfig> dbConfigProperty;

  public ConnectDatabaseHandler(ObjectProperty<DbConfig> dbConfigProperty) {
    this.dbConfigProperty = dbConfigProperty;
    setEnable(dbConfigProperty.getValue() != null);

    this.dbConfigProperty.addListener(observable -> {
      DbConfig dbConfig = dbConfigProperty.getValue();
      if (dbConfig == null) {
        setConfig(0);
        setEnable(false);
      } else if (dbConfig.getConnectionProvider().isConnected()) {
        setConfig(1);
        setEnable(true);
      } else {
        setConfig(0);
        setEnable(true);
      }
    });
  }

  @Override
  public void handle(ActionEvent e) {
    if (dbConfigProperty.getValue() != null) {
      DbConfig dbConfig = dbConfigProperty.getValue();
      if (!dbConfig.getConnectionProvider().isConnected()) {
        Thread th = new Thread(new DbConfig.Reload(dbConfig));
        th.start();
        // dbConfig.reloadStructure();
        dbConfigProperty.setValue(null);
        dbConfigProperty.setValue(dbConfig);
      } else {
        dbConfig.getConnectionProvider().close(extractStage(e));
        if (!dbConfig.getConnectionProvider().isConnected()) {
          dbConfigProperty.setValue(null);
          dbConfigProperty.setValue(dbConfig);
        }
      }
    }
  }
}
