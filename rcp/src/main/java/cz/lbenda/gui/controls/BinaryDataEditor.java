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

import cz.lbenda.common.*;
import cz.lbenda.gui.ImageViewerFrmController;
import cz.lbenda.rcp.ExceptionMessageFrmController;
import cz.lbenda.rcp.IconFactory;
import cz.lbenda.rcp.localization.Message;
import cz.lbenda.rcp.localization.MessageFactory;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.*;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 26.9.15.
 * Give ability to edit the binary data */
@SuppressWarnings("unused")
public class BinaryDataEditor extends Control {

  private static final Logger LOG = LoggerFactory.getLogger(BinaryDataEditor.class);

  public final static String DEFAULT_STYLE_CLASS = "binary-data-editor";

  @Message
  public static final String NULL = "NULL  ";
  @Message
  public static final String NULL_TOOLTIP = "The value is not set";
  @Message
  public static final String SIZE = "%s  ";
  @Message
  public static final String SAVE_TO_FILE_TOOLTIP = "Save to file";
  @Message
  public static final String LOAD_FROM_FILE_TOOLTIP = "Load from file";
  @Message
  public static final String LOAD_FROM_CLIPBOARD = "Load from clipboard";
  @Message
  public static final String SAVE_TO_CLIPBOARD = "Save to clipboard";
  @Message
  public static final String OPEN_AS_IMAGE = "Open as image";
  @Message
  public static final String OPEN_AS_HEX = "Open as hexadecimal text";

  static {
    MessageFactory.initializeMessages(BinaryDataEditor.class);
  }

  private final ObjectProperty<BinaryData> binaryData = new SimpleObjectProperty<>();
  private final ObjectProperty<Long> currentSize = new SimpleObjectProperty<>();
  private final BooleanProperty editable = new SimpleBooleanProperty(false);

  public BinaryDataEditor(@Nonnull BinaryData binaryData, boolean editable) {
    this.editable.set(editable);
    getStyleClass().setAll(DEFAULT_STYLE_CLASS);
    this.binaryData.addListener((observable, oldValue, newValue) -> {
          currentSize.setValue(newValue.isNull() ? null : newValue.size());
        }
    );
    this.binaryData.setValue(binaryData);
  }

  @Override
  protected Skin<?> createDefaultSkin() {
    return new BinaryDataEditorSkin(this);
  }

  public void setBinaryData(@Nonnull BinaryData data) { binaryData.setValue(data); }
  public @Nonnull BinaryData getBinaryData() { return binaryData.getValue(); }
  public ObjectProperty<BinaryData> binaryDataProperty() { return binaryData; }

  public void setEditable(boolean editable) { this.editable.set(editable); }
  public boolean isEditable() { return this.editable.get(); }
  public BooleanProperty editableProperty() { return editable; }

  /** Return current size of binary data */
  public long getCurrentSize() {
    if (currentSize.getValue() == null) { return 0; }
    return currentSize.get();
  }
  public ObservableValue<Long> currentSizeProperty() { return currentSize; }

  public static class BinaryDataEditorSkin extends SkinBase<BinaryDataEditor> {

    private Label sizeLabel = new Label();

    private EventHandler<ActionEvent> saveToFileHandler = event -> {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle(SAVE_TO_FILE_TOOLTIP);
      fileChooser.getExtensionFilters().addAll(Constants.allFilesFilter);
      File file = fileChooser.showSaveDialog(((Node) event.getSource()).getScene().getWindow());
      if (file != null) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
          IOUtils.copy(getSkinnable().getBinaryData().getInputStream(), fos);
        } catch (IOException e) {
          ExceptionMessageFrmController.showException("The file is isn't writable.", e);
        }
      }
    };
    private EventHandler<ActionEvent> loadFromFileHandler = event -> {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle(LOAD_FROM_FILE_TOOLTIP);
      fileChooser.getExtensionFilters().addAll(Constants.allFilesFilter);
      File file = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
      if (file != null) {
        try (FileInputStream fis = new FileInputStream(file)) {
          getSkinnable().setBinaryData(new ByteArrayBinaryData(
              getSkinnable().getBinaryData().getName(),
              IOUtils.toByteArray(fis)));
        } catch (IOException e) {
          ExceptionMessageFrmController.showException("The file is isn't readable.", e);
        }
      }
    };
    private EventHandler<ActionEvent> saveToClipboardHandler = event -> {
      Clipboard clipboard = Clipboard.getSystemClipboard(); // TODO
      Map<DataFormat, Object> map = new HashMap<>();
      byte[] bytes = new byte[0];
      try {
        bytes = IOUtils.toByteArray(getSkinnable().getBinaryData().getInputStream());
        map.put(DataFormat.lookupMimeType("application/octet-stream"), bytes);
        clipboard.setContent(map);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
    private EventHandler<ActionEvent> loadFromClipboardHandler = event -> {
      Clipboard clipboard = Clipboard.getSystemClipboard(); // TODO
      Set<DataFormat> dtfs = clipboard.getContentTypes();
      for (DataFormat df : dtfs) {
        Object content = clipboard.getContent(df);
        System.out.println(df + " : " + content.getClass());
      }
    };
    private EventHandler<ActionEvent> openAsImageHandler = event ->
      ImageViewerFrmController.openImageWindow((Stage) ((Node) event.getSource()).getScene().getWindow(),
          new Image(getSkinnable().getBinaryData().getInputStream()));
    private EventHandler<ActionEvent> openAsHexHandler = event -> System.out.println("TODO"); // TODO

    protected BinaryDataEditorSkin(BinaryDataEditor binaryDataEditor) {
      super(binaryDataEditor);
      HBox pane = new HBox();
      pane.setAlignment(Pos.CENTER_LEFT);

      updateLabelSize();
      binaryDataEditor.currentSize.addListener(observable -> updateLabelSize());

      pane.getChildren().add(sizeLabel);
      if (!binaryDataEditor.getBinaryData().isText()) {
        Button saveToFile = new Button(null, IconFactory.getInstance().imageView(BinaryDataEditor.class, "document-save.png",
            IconFactory.IconLocation.LOCAL_TOOLBAR));
        saveToFile.setTooltip(new Tooltip(SAVE_TO_FILE_TOOLTIP));
        saveToFile.setOnAction(saveToFileHandler);
        Button openFromFile = new Button(null, IconFactory.getInstance().imageView(BinaryDataEditor.class, "document-open.png",
            IconFactory.IconLocation.LOCAL_TOOLBAR));
        openFromFile.setTooltip(new Tooltip(LOAD_FROM_FILE_TOOLTIP));
        openFromFile.setOnAction(loadFromFileHandler);
        Button saveToClipboard = new Button(null, IconFactory.getInstance().imageView(BinaryDataEditor.class, "clipboard-save.png",
            IconFactory.IconLocation.LOCAL_TOOLBAR));
        saveToClipboard.setTooltip(new Tooltip(SAVE_TO_CLIPBOARD));
        saveToClipboard.setOnAction(saveToClipboardHandler);
        Button loadFromClipboard = new Button(null, IconFactory.getInstance().imageView(BinaryDataEditor.class, "clipboard-open.png",
            IconFactory.IconLocation.LOCAL_TOOLBAR));
        loadFromClipboard.setTooltip(new Tooltip(LOAD_FROM_CLIPBOARD));
        loadFromClipboard.setOnAction(loadFromClipboardHandler);
        Button openAsImage = new Button(null, IconFactory.getInstance().imageView(BinaryDataEditor.class, "picture.png",
            IconFactory.IconLocation.LOCAL_TOOLBAR));
        openAsImage.setTooltip(new Tooltip(OPEN_AS_IMAGE));
        openAsImage.setOnAction(openAsImageHandler);
        Button openAsHex = new Button(null, IconFactory.getInstance().imageView(BinaryDataEditor.class, "hex.png",
            IconFactory.IconLocation.LOCAL_TOOLBAR));
        openAsHex.setTooltip(new Tooltip(OPEN_AS_HEX));
        openAsHex.setOnAction(openAsHexHandler);
        loadFromClipboard.setDisable(true);
        openAsHex.setDisable(true);
        saveToClipboard.setDisable(true);
        openFromFile.setDisable(!binaryDataEditor.isEditable());
        // loadFromClipboard.setDisable(!binaryDataEditor.isEditable());
        // openAsHex.setDisable(binaryDataEditor.getBinaryData() == null || binaryDataEditor.getCurrentSize() == 0);
        openAsImage.setDisable(binaryDataEditor.getCurrentSize() == 0);
        // saveToClipboard.setDisable(binaryDataEditor.getBinaryData() == null || binaryDataEditor.getCurrentSize() == 0);
        saveToFile.setDisable(binaryDataEditor.getCurrentSize() == 0);
        pane.getChildren().addAll(saveToFile, openFromFile, saveToClipboard, loadFromClipboard, openAsImage, openAsHex);

        binaryDataEditor.binaryDataProperty().addListener((observable, oldValue, newValue) -> {
          // openAsHex.setDisable(newValue == null || newValue.size() == 0);
          openAsImage.setDisable(newValue == null || newValue.size() == 0);
          // saveToClipboard.setDisable(newValue == null || newValue.size() == 0);
          // saveToFile.setDisable(newValue == null || newValue.size() == 0);
        });
        binaryDataEditor.editableProperty().addListener((observable, oldValue, newValue) -> {
          openFromFile.setDisable(!newValue);
          loadFromClipboard.setDisable(!newValue);
        });
      } else {
        Button openAsText = TextAreaFrmController.createOpenButton(binaryDataEditor.getBinaryData().getName(),
            () -> {
              try {
                return IOUtils.toString(getSkinnable().getBinaryData().getReader());
              } catch (IOException e) {
                LOG.error("The reader is unreadable", e);
                return e.toString();
              }
            },
            (text) -> getSkinnable().setBinaryData(new CharSequenceBinaryData(getSkinnable().getBinaryData().getName(), text))
        );
        pane.getChildren().add(openAsText);
      }
      getChildren().add(pane);
    }

    private void updateLabelSize() {
      if (sizeLabel.getTooltip() == null) { sizeLabel.setTooltip(new Tooltip()); }
      if (getSkinnable().getBinaryData().isNull()) {
        sizeLabel.setText(NULL);
        sizeLabel.getTooltip().setText(NULL_TOOLTIP);
      } else {
        Long size = getSkinnable().getCurrentSize();
        sizeLabel.setText(String.format(SIZE, Constants.transformToPrefixedSize(size) + Constants.prefixForSize(size)));
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.getDefault());
        sizeLabel.getTooltip().setText(format.format(size));
      }
    }
  }
}
