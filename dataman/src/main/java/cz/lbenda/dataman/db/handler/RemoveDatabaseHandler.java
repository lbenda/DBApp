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
import cz.lbenda.dataman.rc.DbConfigFactory;
import cz.lbenda.rcp.action.AbstractAction;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import cz.lbenda.rcp.localization.MessageFactory;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Action which copy current database config */
@ActionConfig(
    category = "/DbConfig/sessions",
    id = "cz.lbenda.dataman.db.handler.RemoveDatabaseHandler",
    priority = 500,
    gui = @ActionGUIConfig(
        iconBase = "database-remove.png",
        displayName = @Message(id="Remove_database", msg="Remove"),
        displayTooltip = @Message(id="Remove_database_tooltip", msg="Remove database config")
    )
)
public class RemoveDatabaseHandler extends AbstractAction {

  @Message
  public static final String msgRemoveTitle = "Remove confirm";
  @Message
  public static final String msgRemoveContent = "The configuration '%s' will be removed.\nAre you sure, you want is?";

  static {
    MessageFactory.initializeMessages(RemoveDatabaseHandler.class);
  }

  /** The holder to which is set session configuration values */
  private ObjectProperty<DbConfig> dbConfigObserver;

  public RemoveDatabaseHandler(ObjectProperty<DbConfig> dbConfigObserver) {
    this.dbConfigObserver = dbConfigObserver;
    setEnable(dbConfigObserver.getValue() != null);
    dbConfigObserver.addListener(observable -> {
      setEnable(dbConfigObserver.getValue() != null);
    });
  }

  @Override
  public void handle(ActionEvent e) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle(msgRemoveTitle);
    alert.setContentText(String.format(msgRemoveContent, dbConfigObserver.get().getId()));
    Optional<ButtonType> result = alert.showAndWait();
    if  (result.isPresent() && result.get() == ButtonType.OK) {
      DbConfigFactory.removeConfiguration(dbConfigObserver.get());
      DbConfigFactory.saveConfiguration();
    }
  }
}