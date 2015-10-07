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

import cz.lbenda.common.Constants;
import cz.lbenda.rcp.IconFactory;
import cz.lbenda.rcp.localization.Message;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 26.9.15.
 * Text field with button for choose file */
@SuppressWarnings("unused")
public class FileField extends EditFieldWithButton {

  private static final Logger LOG = LoggerFactory.getLogger(FileField.class);

  public final static String DEFAULT_STYLE_CLASS = "file-field";

  @Message
  public static final String BTN_TOOLTIP = "Choose file";
  @Message
  public static final String CHOOSE_FILE_WINDOW_TITLE = "Choose file";

  /** Flag for choose Save dialog or Open dialog */
  private final BooleanProperty save = new SimpleBooleanProperty(true);

  public FileField() {
    this(null, true, true);
  }

  public FileField(File file, boolean editable, boolean save) {
    super();
    getStyleClass().setAll(DEFAULT_STYLE_CLASS);
    setEditable(editable);
    this.save.set(save);
  }

  /** Set field to save/open mode - is open save or open dialog */
  public void setSave(boolean save) { this.save.set(save); }
  /** Field in save/open mode - is open save or open dialog */
  public boolean isSave() { return this.save.get(); }
  /** Field in save/open mode - is open save or open dialog */
  public BooleanProperty saveProperty() { return save; }

  protected String buttonTooltip() {
    return BTN_TOOLTIP;
  }
  protected ImageView buttonImage() {
    return IconFactory.getInstance().imageView(FileField.class, "threeDots.png",
        IconFactory.IconLocation.LOCAL_TOOLBAR);
  }
  protected EventHandler<ActionEvent> buttonEventHandler() {
    return event -> {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle(CHOOSE_FILE_WINDOW_TITLE);
      fileChooser.getExtensionFilters().addAll(Constants.allFilesFilter);
      if (!StringUtils.isBlank(getText())) {
        fileChooser.setInitialFileName(getText());
        File file = new File(getText());
        if (file.isDirectory()) { fileChooser.setInitialDirectory(file); }
        else { fileChooser.setInitialDirectory(file.getParentFile()); }
      }
      File file;
      if (isSave()) { file = fileChooser.showSaveDialog(((Node) event.getSource()).getScene().getWindow()); }
      else { file = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow()); }
      if (file != null) { setText(file.getAbsolutePath()); }
    };
  }
}
