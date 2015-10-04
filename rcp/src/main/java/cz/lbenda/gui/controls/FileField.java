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
package cz.lbenda.gui.controls;

import cz.lbenda.common.AbstractHelper;
import cz.lbenda.common.Constants;
import cz.lbenda.rcp.IconFactory;
import cz.lbenda.rcp.localization.Message;
import cz.lbenda.rcp.localization.MessageFactory;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 26.9.15.
 * Text field with button for choose file */
@SuppressWarnings("unused")
public class FileField extends Control {

  private static final Logger LOG = LoggerFactory.getLogger(FileField.class);

  public final static String DEFAULT_STYLE_CLASS = "file-field";

  @Message
  public static final String BTN_TOOLTIP = "Choose file";
  @Message
  public static final String CHOOSE_FILE_WINDOW_TITLE = "Choose file";

  static {
    MessageFactory.initializeMessages(FileField.class);
  }

  private final ObjectProperty<String> file = new SimpleObjectProperty<>();
  private final BooleanProperty editable = new SimpleBooleanProperty(false);
  /** Flag for choose Save dialog or Open dialog */
  private final BooleanProperty save = new SimpleBooleanProperty(true);

  public FileField() {
    this(null, true, true);
  }

  public FileField(File file, boolean editable, boolean save) {
    this.editable.set(editable);
    getStyleClass().setAll(DEFAULT_STYLE_CLASS);
    this.save.set(save);
  }

  @Override
  protected Skin<?> createDefaultSkin() {
    return new FileFieldSkin(this);
  }

  /** Absolute path to choose file */
  public void setFile(String file) { this.file.setValue(file); }
  /** Absolute path to choose file */
  public String getFile() { return file.getValue(); }
  /** Absolute path to choose file */
  public ObjectProperty<String> fileProperty() { return file; }

  /** Set filed to editable mode  */
  public void setEditable(boolean editable) { this.editable.set(editable); }
  /** Filed in editable mode  */
  public boolean isEditable() { return this.editable.get(); }
  /** Filed in editable mode  */
  public BooleanProperty editableProperty() { return editable; }

  /** Set field to save/open mode - is open save or open dialog */
  public void setSave(boolean save) { this.save.set(save); }
  /** Field in save/open mode - is open save or open dialog */
  public boolean isSave() { return this.save.get(); }
  /** Field in save/open mode - is open save or open dialog */
  public BooleanProperty saveProperty() { return save; }

  public static class FileFieldSkin extends SkinBase<FileField> {

    private Label sizeLabel = new Label();

    private EventHandler<ActionEvent> chooseFile = event -> {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle(CHOOSE_FILE_WINDOW_TITLE);
      fileChooser.getExtensionFilters().addAll(Constants.allFilesFilter);
      if (!StringUtils.isBlank(getSkinnable().getFile())) {
        fileChooser.setInitialFileName(getSkinnable().getFile());
        File file = new File(getSkinnable().getFile());
        if (file.isDirectory()) { fileChooser.setInitialDirectory(file); }
        else { fileChooser.setInitialDirectory(file.getParentFile()); }
      }
      File file;
      if (getSkinnable().isSave()) { file = fileChooser.showSaveDialog(((Node) event.getSource()).getScene().getWindow()); }
      else { file = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow()); }
      if (file != null) { getSkinnable().setFile(file.getAbsolutePath()); }
    };

    protected FileFieldSkin(FileField fileField) {
      super(fileField);
      HBox pane = new HBox();
      pane.setAlignment(Pos.CENTER_LEFT);

      TextField tf = new TextField();
      tf.setText(fileField.getFile());
      tf.setEditable(fileField.isEditable());
      Button button = new Button();
      button.setDisable(!fileField.isEditable());
      button.setTooltip(new Tooltip(BTN_TOOLTIP));
      button.setGraphic(IconFactory.getInstance().imageView(FileField.class, "threeDots.png",
          IconFactory.IconLocation.LOCAL_TOOLBAR));
      pane.getChildren().addAll(tf, button);
      getChildren().add(pane);

      fileField.editableProperty().addListener((observable, oldValue, newValue) -> {
        tf.setEditable(newValue);
        button.setDisable(!newValue);
      });

      button.setOnAction(chooseFile);
      tf.textProperty().addListener((observable, oldValue, newValue) -> getSkinnable().setFile(newValue));
      getSkinnable().fileProperty().addListener((observable, oldValue, newValue) -> {
        if (!AbstractHelper.nullEquals(oldValue, newValue)) { tf.textProperty().setValue(newValue); }
      });
    }
  }
}
