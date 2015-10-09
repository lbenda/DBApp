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
import cz.lbenda.dataman.db.frm.DbConfigFrmController;
import cz.lbenda.rcp.action.AbstractAction;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * add new database handler */
@ActionConfig(
    category = "/DbConfig/sessions",
    id = "cz.lbenda.dataman.db.handler.ReloadDatabaseHandler",
    priority = 800,
    gui = @ActionGUIConfig(
        iconBase = "database-reload.png",
        displayName = @Message(id="Reload_database", msg="Reload"),
        displayTooltip = @Message(id="Reload_database_tooltip", msg="Reload database")
    )
)
public class ReloadDatabaseHandler extends AbstractAction {

  private static Logger LOG = LoggerFactory.getLogger(ReloadDatabaseHandler.class);

  /** The holder to which is set session configuration values */
  private ObjectProperty<DbConfig> dbConfigObserver;

  public ReloadDatabaseHandler(ObjectProperty<DbConfig> dbConfigObserver) {
    this.dbConfigObserver = dbConfigObserver;
    setEnable(dbConfigObserver.getValue() != null);
    dbConfigObserver.addListener((observable, oldValue, newValue) -> {
      setEnable(newValue != null);
    });
  }

  @Override
  public void handle(ActionEvent e) {
    dbConfigObserver.getValue().reloadStructure();
  }
}