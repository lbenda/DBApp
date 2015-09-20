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

import cz.lbenda.dataman.db.frm.DbConfigFrmController;
import cz.lbenda.dataman.rc.DbConfig;
import cz.lbenda.rcp.action.AbstractAction;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import javafx.event.ActionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * add new database handler */
@ActionConfig(
    category = "/DbConfig/sessions",
    id = "cz.lbenda.dataman.db.handler.AddDatabaseHandler",
    priority = 300,
    gui = @ActionGUIConfig(
        iconBase = "database-add.png",
        displayName = @Message(id="New_database", msg="New"),
        displayTooltip = @Message(id="New_database_tooltip", msg="Create new database")
    )
)
public class AddDatabaseHandler extends AbstractAction {

  private static Logger LOG = LoggerFactory.getLogger(AddDatabaseHandler.class);

  public AddDatabaseHandler() {
  }

  @Override
  public void handle(ActionEvent e) {
    DbConfig sc = DbConfigFrmController.openDialog(null);
  }
}