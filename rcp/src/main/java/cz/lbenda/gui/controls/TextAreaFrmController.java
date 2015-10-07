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
import cz.lbenda.common.Tuple2;
import cz.lbenda.gui.editor.*;
import cz.lbenda.rcp.DialogHelper;
import cz.lbenda.rcp.ExceptionMessageFrmController;
import cz.lbenda.rcp.IconFactory;
import cz.lbenda.rcp.localization.Message;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 21.9.15. */
public class TextAreaFrmController implements Initializable {

  private static Logger LOG = LoggerFactory.getLogger(TextAreaFrmController.class);
  private static final String FXML_RESOURCE = "TextAreaFrm.fxml";

  @Message
  public final static String msgDefaultWindowTitle = "Text editor";
  @Message
  public final static String msgBtnOpenInEditor_tooltip = "Open in editor window";
  @Message
  public final static String msgLoadFile_title = "Open file";
  @Message
  public final static String msgSaveFile_title = "Save to file";

  /** Image for button which open text editor */
  public static final Image BUTTON_IMAGE = IconFactory.getInstance().image(TextFieldArea.class, "document-edit.png",
      IconFactory.IconLocation.INDICATOR);

  @FXML
  private BorderPane mainPane; public BorderPane getMainPane() { return mainPane; }
  @FXML
  private Button btnLoad;
  @FXML
  private Button btnSave;
  @FXML
  private Button btnSaveAs;
  @FXML
  private ComboBox<String> textType;
  @FXML
  private Button btnOk;
  @FXML
  private Button btnCancel;

  private StringProperty text = new SimpleStringProperty(); public StringProperty textProperty() { return text; }
  private TextEditor textEditor; /* public TextEditor getTextEditor() { return textEditor; }*/
  private File lastFile;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    textEditor = new TextEditor();
    mainPane.setCenter(textEditor.createCodeArea());
    text.addListener((observer, oldValue, newValue) -> textEditor.changeText(newValue));

    FileChooser fileChooser = new FileChooser();
    fileChooser.getExtensionFilters().addAll(Constants.allFilesFilter);
    btnLoad.setOnAction(event -> {
      fileChooser.setTitle(msgLoadFile_title);
      if (lastFile != null) { fileChooser.setInitialDirectory(lastFile.getParentFile()); }
      File file = fileChooser.showOpenDialog(mainPane.getScene().getWindow());
      if (file != null) {
        lastFile = file;
        try (FileReader reader = new FileReader(file)) {
          textEditor.changeText(IOUtils.toString(reader));
        } catch (IOException e) {
          ExceptionMessageFrmController.showException("The file " + lastFile.getAbsolutePath() + " isn't openable.", e);
        }
      }
    });
    btnSave.setOnAction(event -> {
      File file = lastFile;
      if (lastFile == null) {
        fileChooser.setTitle(msgSaveFile_title);
        file = fileChooser.showSaveDialog(mainPane.getScene().getWindow());
      }
      if (file != null) {
        lastFile = file;
        try (OutputStream writer = new FileOutputStream(lastFile)) {
          IOUtils.copy(IOUtils.toInputStream(textEditor.getText()), writer);
        } catch (IOException e) {
          ExceptionMessageFrmController.showException("The file " + lastFile.getAbsolutePath() + " isn't writable.", e);
        }
      }
    });
    btnSaveAs.setOnAction(event -> {
      fileChooser.setTitle(msgSaveFile_title);
      if (lastFile != null) { fileChooser.setInitialDirectory(lastFile.getParentFile()); }
      File file = fileChooser.showSaveDialog(mainPane.getScene().getWindow());
      if (file != null) {
        lastFile = file;
        try (OutputStream writer = new FileOutputStream(lastFile)) {
          IOUtils.copy(IOUtils.toInputStream(textEditor.getText()), writer);
        } catch (IOException e) {
          ExceptionMessageFrmController.showException("The file " + lastFile.getAbsolutePath() + " isn't writable.", e);
        }
      }
    });

    btnCancel.setOnAction(event -> ((Stage) btnCancel.getScene().getWindow()).close());
    btnOk.setOnAction(event -> {
      text.setValue(textEditor.getText());
      ((Stage) btnCancel.getScene().getWindow()).close();
    });

    textType.getSelectionModel().select(0);
    textType.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if ("Plain".equals(newValue)) { textEditor.changeHighlighter(new HighlighterPlain()); }
      else if ("SQL".equals(newValue)) { textEditor.changeHighlighter(new HighlighterSQL()); }
      else if ("XML".equals(newValue)) { textEditor.changeHighlighter(new HighlighterXml()); }
      else if ("Java".equals(newValue)) { textEditor.changeHighlighter(new HighlighterJava()); }
    });
  }

  /** Create new instance return main node and controller of this node and sub-nodes */
  public static Tuple2<Parent, TextAreaFrmController> createNewInstance() {
    URL resource = TextAreaFrmController.class.getResource(FXML_RESOURCE);
    try {
      FXMLLoader loader = new FXMLLoader();
      loader.setLocation(resource);
      loader.setBuilderFactory(new JavaFXBuilderFactory());
      Parent node = loader.load(resource.openStream());
      TextAreaFrmController controller = loader.getController();
      return new Tuple2<>(node, controller);
    } catch (IOException e) {
      LOG.error("Problem with reading FXML", e);
      throw new RuntimeException("Problem with reading FXML", e);
    }
  }

  /** Create button which can open text editor */
  public static Button createOpenButton(String windowTitle,
                                        @Nonnull Supplier<String> oldValueSupplier,
                                        @Nonnull Consumer<String> newValueConsumer) {
    String title = windowTitle == null ? msgDefaultWindowTitle : windowTitle;
    Button result = new Button(null, new ImageView(BUTTON_IMAGE));
    result.setTooltip(new Tooltip(msgBtnOpenInEditor_tooltip));
    BorderPane.setAlignment(result, Pos.TOP_RIGHT);
    result.setOnAction(event -> {
      Tuple2<Parent, TextAreaFrmController> tuple2 = TextAreaFrmController.createNewInstance();
      tuple2.get2().textProperty().setValue(oldValueSupplier.get());
      tuple2.get2().textProperty().addListener((observable, oldValue, newValue) -> newValueConsumer.accept(newValue));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      DialogHelper.getInstance().openWindowInCenterOfStage(stage, tuple2.get2().getMainPane(), title);
    });
    return result;
  }

  public static EventHandler<ActionEvent> openEventHandler(String windowTitle,
                                                           @Nonnull Supplier<String> oldValueSupplier,
                                                           @Nonnull Consumer<String> newValueConsumer) {
    String title = windowTitle == null ? msgDefaultWindowTitle : windowTitle;
    return event -> {
      Tuple2<Parent, TextAreaFrmController> tuple2 = TextAreaFrmController.createNewInstance();
      tuple2.get2().textProperty().setValue(oldValueSupplier.get());
      tuple2.get2().textProperty().addListener((observable, oldValue, newValue) -> newValueConsumer.accept(newValue));
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      DialogHelper.getInstance().openWindowInCenterOfStage(stage, tuple2.get2().getMainPane(), title);
    };
  }
}
