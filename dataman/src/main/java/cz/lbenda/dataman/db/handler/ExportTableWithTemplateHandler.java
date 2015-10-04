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

import cz.lbenda.dataman.db.ExportTableData;
import cz.lbenda.dataman.db.ExportTableData.TemplateExportConfig;
import cz.lbenda.dataman.db.SQLQueryRows;
import cz.lbenda.dataman.db.frm.ChooseExportTemplateFrmController;
import cz.lbenda.rcp.ExceptionMessageFrmController;
import cz.lbenda.rcp.action.AbstractAction;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Export current db configuration */
@ActionConfig(
    category = "/ExportImport/tableExport",
    id = "cz.lbenda.dataman.db.handler.ExportTableWithTemplateExcelHandler",
    priority = 200,
    gui = @ActionGUIConfig(
        iconBase = "database-exportWithTemplate.png",
        displayName = @Message(id="Export_tableWithTemplate", msg="With template"),
        displayTooltip = @Message(id="Export_tableWithTemplate_tooltip", msg="Export with template")
    )
)
public class ExportTableWithTemplateHandler extends AbstractAction {

  private static final Logger LOG = LoggerFactory.getLogger(ExportTableWithTemplateHandler.class);

  /** Loaded query */
  private ObjectProperty<SQLQueryRows> sqlQueryRowsObjectProperty;
  /** Last get template export config */
  private TemplateExportConfig templateExportConfig;

  public ExportTableWithTemplateHandler(ObjectProperty<SQLQueryRows> sqlQueryRowsObjectProperty) {
    this.sqlQueryRowsObjectProperty = sqlQueryRowsObjectProperty;
    setEnable(sqlQueryRowsObjectProperty.getValue() != null);
    this.sqlQueryRowsObjectProperty.addListener(observable -> {
      setEnable(sqlQueryRowsObjectProperty.getValue() != null);
    });
  }

  @Override
  public void handle(ActionEvent event) {
    if (sqlQueryRowsObjectProperty.getValue() == null) { return; }
    templateExportConfig = ChooseExportTemplateFrmController.openDialog(templateExportConfig);
    if (templateExportConfig != null && StringUtils.isNoneBlank(templateExportConfig.getFile())
        && StringUtils.isNoneBlank(templateExportConfig.getTemplateFile())) {
      SQLQueryRows sqlQueryRows = sqlQueryRowsObjectProperty.getValue();
      try (FileOutputStream fos = new FileOutputStream(templateExportConfig.getFile())) {
        ExportTableData.writeSqlQueryRows(templateExportConfig.getTemplateFormat(), sqlQueryRows,
            templateExportConfig.getTemplateFile(), fos);
      } catch (IOException e) {
        LOG.error("Problem with write files", e);
        ExceptionMessageFrmController.showException("Problem with write files", e);
      }
    }
  }
}
