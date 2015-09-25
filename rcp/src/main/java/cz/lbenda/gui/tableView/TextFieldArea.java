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

import cz.lbenda.common.Constants;
import cz.lbenda.common.Tuple2;
import cz.lbenda.rcp.DialogHelper;
import cz.lbenda.rcp.IconFactory;
import cz.lbenda.rcp.localization.Message;
import cz.lbenda.rcp.localization.MessageFactory;
import javafx.beans.property.*;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 24.9.15.
 * element which show text field or text area by size of field */
@SuppressWarnings("unused")
public class TextFieldArea {

  @Message
  public final static String msgBtnOpenInEditor_tooltip = "Open in editor windo";
  static {
    MessageFactory.initializeMessages(TextFieldArea.class);
  }

  private final BorderPane panel = new BorderPane(); public Node getNode() { return panel; }
  private final TextInputControl textInputControl;
  /** Property which set component to editable, or not editable mode */
  private final BooleanProperty editable = new SimpleBooleanProperty(false);
  /** Property which hold actual value of component. No value in text field, but value which was commit or set by setText. */
  private final StringProperty text = new SimpleStringProperty();
  /** Flag which configure what to do when textInputControl lost focus (and no edit window is open). If cancel will be called, or commit. */
  private final BooleanProperty cancelOnFocusLost = new SimpleBooleanProperty(false);

  /** Construct of text field
   * @param windowTitle title of window if is open
   * @param useTextField use text field instead of text area */
  public TextFieldArea(String windowTitle, boolean useTextField) {
    this.textInputControl = useTextField ? new TextField() : new TextArea();
    if (!useTextField) { textInputControl.setPrefHeight(Constants.TEXT_AREA_PREF_HIGH); }
    this.panel.setCenter(textInputControl);
    BorderPane.setAlignment(textInputControl, Pos.TOP_LEFT);
    Button btOpenTextEditor = new Button(null,
        IconFactory.getInstance().imageView(this, "document-edit.png", IconFactory.IconLocation.INDICATOR));
    btOpenTextEditor.setTooltip(new Tooltip(msgBtnOpenInEditor_tooltip));
    this.panel.setRight(btOpenTextEditor);
    BorderPane.setAlignment(btOpenTextEditor, Pos.TOP_RIGHT);

    this.textInputControl.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      switch (event.getCode()) {
        case ENTER:
        case TAB:
          this.commitEdit(textInputControl.getText());
          break;
        case ESCAPE:
          this.cancelEdit();
          break;
      }
    });
    this.textInputControl.focusedProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        if (isCancelOnFocusLost()) { cancelEdit(); }
        else { commitEdit(textInputControl.getText()); }
      }
    });
    btOpenTextEditor.setOnAction(event -> {
      Tuple2<Parent, TextAreaFrmController> tuple2 = TextAreaFrmController.createNewInstance();
      tuple2.get2().textProperty().setValue(textInputControl.getText());
      tuple2.get2().textProperty().addListener((observable, oldValue, newValue) -> textInputControl.setText(newValue));
      DialogHelper.getInstance().openWindowInCenterOfStage((Stage) textInputControl.getScene().getWindow(),
          tuple2.get2().getMainPane(), windowTitle);
    });
    editable.addListener((observable, oldValue, newValue) -> textInputControl.editableProperty().setValue(newValue));
    text.addListener((observable, oldValue, newValue) -> textInputControl.textProperty().setValue(newValue));
  }

  public boolean isEditable() { return editable.get(); }
  public void setEditable(boolean editable) { this.editable.setValue(editable); }
  public BooleanProperty editableProperty() { return editable; }

  public String getText() { return text.getValue(); }
  public void setText(String value) { this.text.setValue(value); }
  public StringProperty textProperty() { return text; }

  public boolean isCancelOnFocusLost() { return cancelOnFocusLost.get(); }
  public void setCancelOnFocusLost(boolean cancelOnFocusLost) { this.cancelOnFocusLost.set(cancelOnFocusLost); }
  public BooleanProperty cancelOnFocusLostProperty() { return cancelOnFocusLost; }

  public void requestFocus() { textInputControl.requestFocus(); }

  /** Commit given value to textProperty */
  public void commitEdit(String newValue) {
    this.setText(newValue);
  }

  /** If you call this method, then value in text field is set from the value, which was last commit or which was
   * set by setText() respectively to textProperty. */
  public void cancelEdit() {
    textInputControl.setText(text.getValue());
  }
}
