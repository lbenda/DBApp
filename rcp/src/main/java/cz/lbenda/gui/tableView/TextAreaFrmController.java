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
package cz.lbenda.gui.tableView;

import cz.lbenda.common.Tuple2;
import cz.lbenda.gui.editor.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 21.9.15. */
public class TextAreaFrmController implements Initializable {

  private static Logger LOG = LoggerFactory.getLogger(TextAreaFrmController.class);
  private static final String FXML_RESOURCE = "TextAreaFrm.fxml";

  @FXML
  private BorderPane mainPane; public BorderPane getMainPane() { return mainPane; }
  @FXML
  private ToggleGroup tgTextType;
  @FXML
  private ToggleButton tbPlain;
  @FXML
  private ToggleButton tbSQL;
  @FXML
  private ToggleButton tbXML;
  @FXML
  private ToggleButton tbJava;
  @FXML
  private Button btnOk;
  @FXML
  private Button btnCancel;

  private StringProperty text = new SimpleStringProperty(); public StringProperty textProperty() { return text; }
  private TextEditor textEditor; /* public TextEditor getTextEditor() { return textEditor; }*/

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    textEditor = new TextEditor();
    mainPane.setCenter(textEditor.createCodeArea());
    text.addListener((observer, oldValue, newValue) -> textEditor.changeText(newValue));

    btnCancel.setOnAction(event -> ((Stage) btnCancel.getScene().getWindow()).close());
    btnOk.setOnAction(event -> {
      text.setValue(textEditor.getText());
      ((Stage) btnCancel.getScene().getWindow()).close();
    });
    tgTextType.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == tbPlain) { textEditor.changeHighlighter(new HighlighterPlain()); }
      else if (newValue == tbSQL) { textEditor.changeHighlighter(new HighlighterSQL()); }
      else if (newValue == tbXML) { textEditor.changeHighlighter(new HighlighterXml()); }
      else if (newValue == tbJava) { textEditor.changeHighlighter(new HighlighterJava()); }
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
}
