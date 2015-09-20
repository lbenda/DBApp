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
import cz.lbenda.dataman.db.ExportTableData;
import cz.lbenda.dataman.db.SQLQueryRows;
import cz.lbenda.rcp.DialogHelper;
import cz.lbenda.rcp.ExceptionMessageFrmController;
import cz.lbenda.rcp.action.AbstractAction;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import cz.lbenda.rcp.localization.MessageFactory;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Export current db configuration */
@ActionConfig(
    category = "/ExportImport/tableExport",
    id = "cz.lbenda.dataman.db.handler.ExportTableExcelHandler",
    priority = 100,
    gui = @ActionGUIConfig(
        iconBase = "database-export.png",
        displayName = @Message(id="Export_table", msg="Export"),
        displayTooltip = @Message(id="Export_table_excel_tooltip", msg="Export current table to XSLX")
    )
)
public class ExportTableHandler extends AbstractAction {

  private static final Logger LOG = LoggerFactory.getLogger(ExportTableHandler.class);

  /** Loaded query */
  private ObjectProperty<SQLQueryRows> sqlQueryRowsObjectProperty;

  @Message
  private static final String msgFileChooseTitle = "Choose file to which XLSX is saved";
  static {
    MessageFactory.initializeMessages(ExportTableHandler.class);
  }

  public ExportTableHandler(ObjectProperty<SQLQueryRows> sqlQueryRowsObjectProperty) {
    this.sqlQueryRowsObjectProperty = sqlQueryRowsObjectProperty;
    setEnable(sqlQueryRowsObjectProperty.getValue() != null);
    this.sqlQueryRowsObjectProperty.addListener(observable -> {
      setEnable(sqlQueryRowsObjectProperty.getValue() != null);
    });
  }

  @Override
  public void handle(ActionEvent event) {
    if (sqlQueryRowsObjectProperty.getValue() == null) { return; }
    SQLQueryRows sqlQueryRows = sqlQueryRowsObjectProperty.getValue();
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle(msgFileChooseTitle);
    Node node = (Node) event.getSource();
    fileChooser.getExtensionFilters().addAll(Constants.spreadSheetFilter);
    File file = DialogHelper.getInstance().canBeOverwriteDialog(
        fileChooser.showSaveDialog(node.getScene().getWindow()),
        Constants.CSV_EXTENSION);
    if (file != null) {
      try (FileOutputStream fos = new FileOutputStream(file)) {
        ExportTableData.writeSqlQueryRows(file.getName(), sqlQueryRows, Constants.EXPORT_SHEET_NAME, fos);
      } catch (IOException e) {
        LOG.error("Problem with write files", e);
        ExceptionMessageFrmController.showException("Problem with write files", e);
      }
    }
  }
}
