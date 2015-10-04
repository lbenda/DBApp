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
package cz.lbenda.dataman.db.frm;

import cz.lbenda.common.Tuple2;
import cz.lbenda.dataman.db.ExportTableData;
import cz.lbenda.dataman.db.ExportTableData.TemplateExportConfig;
import cz.lbenda.gui.controls.FileField;
import cz.lbenda.rcp.DialogHelper;
import cz.lbenda.rcp.localization.Message;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 4.10.15.
 * Frame controller which is used for choose template and template system for exporting data */
public class ChooseExportTemplateFrmController implements Initializable {

  private static Logger LOG = LoggerFactory.getLogger(ChooseExportTemplateFrmController.class);
  private static final String FXML_RESOURCE = "ChooseExportTemplateFrm.fxml";

  @Message
  public static final String msgDialogTitle = "Export data with template";
  @Message
  public static final String msgDialogHeader = "Choose template engine, template file and file where result will be store.";

  @FXML
  private ComboBox<ExportTableData.TemplateFormat> cbTemplateType;
  @FXML
  private FileField ffTemplateFile;
  @FXML
  private FileField ffFile;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    cbTemplateType.getItems().addAll(ExportTableData.TemplateFormat.values());
  }

  /** Create new instance return main node and controller of this node and subnodes */
  public static Tuple2<Parent, ChooseExportTemplateFrmController> createNewInstance() {
    URL resource = DbConfigFrmController.class.getResource(FXML_RESOURCE);
    try {
      FXMLLoader loader = new FXMLLoader();
      loader.setLocation(resource);
      loader.setBuilderFactory(new JavaFXBuilderFactory());
      Parent node = loader.load(resource.openStream());
      ChooseExportTemplateFrmController controller = loader.getController();
      return new Tuple2<>(node, controller);
    } catch (IOException e) {
      LOG.error("Problem with reading FXML", e);
      throw new RuntimeException("Problem with reading FXML", e);
    }
  }

  public void valuesPreset(final TemplateExportConfig templateExportConfig) {
    this.cbTemplateType.getSelectionModel().select(templateExportConfig.getTemplateFormat());
    this.ffTemplateFile.setFile(templateExportConfig.getTemplateFile());
    this.ffFile.setFile(templateExportConfig.getFile());
  }

  public TemplateExportConfig exportConfig() {
    return new TemplateExportConfig(cbTemplateType.getSelectionModel().getSelectedItem(),
        ffTemplateFile.getFile(), ffFile.getFile());
  }


  public static TemplateExportConfig openDialog(final TemplateExportConfig templateExportConfig) {
    Dialog<TemplateExportConfig> dialog = DialogHelper.createDialog();
    dialog.setResizable(true);
    final Tuple2<Parent, ChooseExportTemplateFrmController> tuple = createNewInstance();
    if (templateExportConfig != null) { tuple.get2().valuesPreset(templateExportConfig); }
    dialog.setTitle(msgDialogTitle);
    dialog.setHeaderText(msgDialogHeader);

    dialog.getDialogPane().setContent(tuple.get1());
    ButtonType buttonTypeOk = ButtonType.OK;
    ButtonType buttonTypeCancel = ButtonType.CANCEL;
    dialog.getDialogPane().getButtonTypes().add(buttonTypeCancel);
    dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);
    dialog.getDialogPane().setPadding(new Insets(0, 0, 0, 0));

    dialog.setResultConverter(b -> b == buttonTypeOk ? tuple.get2().exportConfig() : null);

    Optional<TemplateExportConfig> result = dialog.showAndWait();
    if (result.isPresent()) { return result.get(); }
    return null;
  }
}
