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

import cz.lbenda.dataman.Constants;
import cz.lbenda.dataman.rc.DbConfig;
import cz.lbenda.rcp.DialogHelper;
import cz.lbenda.rcp.action.AbstractAction;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import cz.lbenda.rcp.localization.MessageFactory;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import org.apache.commons.io.FilenameUtils;

import java.io.File;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Export current db configuration */
@ActionConfig(
    category = "/DbConfig/exportImport",
    id = "cz.lbenda.dataman.db.handler.ExportDatabaseHandler",
    priority = 1000,
    gui = @ActionGUIConfig(
        iconBase = "database-export.png",
        displayName = @Message(id="Export_database", msg="Export"),
        displayTooltip = @Message(id="Export_database_tooltip", msg="Export database configuration to file")
    )
)
public class ExportDatabaseHandler extends AbstractAction {

  /** The holder to which is set session configuration values */
  private ObjectProperty<DbConfig> dbConfigProperty;

  @Message
  private String msgFileChooseTitle = "Choose file for configuration save";

  public ExportDatabaseHandler(ObjectProperty<DbConfig> dbConfigProperty) {
    MessageFactory.initializeMessages(this);
    this.dbConfigProperty = dbConfigProperty;
    setEnable(dbConfigProperty.getValue() != null);
    this.dbConfigProperty.addListener(observable -> {
      DbConfig dbConfig = dbConfigProperty.getValue();
      setEnable(dbConfig != null);
    });
  }

  @Override
  public void handle(ActionEvent e) {
    if (dbConfigProperty.getValue() != null) {
      DbConfig dbConfig = dbConfigProperty.getValue();
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle(msgFileChooseTitle);
      Node node = (Node) e.getSource();
      fileChooser.getExtensionFilters().addAll(Constants.configFileFilter);
      File file = fileChooser.showSaveDialog(node.getScene().getWindow());
      if (file != null) {
        if ("".equals(FilenameUtils.getExtension(file.getName()))) {
          file = new File(file.getAbsoluteFile() + "." + Constants.CONFIG_EXTENSION);
          if (DialogHelper.getInstance().canBeOverwriteDialog(file)) {
            dbConfig.save(file);
          }
        } else { dbConfig.save(file); }
      }
    }
  }
}
