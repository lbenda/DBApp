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
import cz.lbenda.rcp.localization.Message;
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 24.9.15.
 * element which show text field or text area by size of field */
@SuppressWarnings("unused")
public class TextFieldArea extends EditFieldWithButton {

  public final static String DEFAULT_STYLE_CLASS = "text-field";

  @Message
  public static final String BUTTON_TOOLTIP = "Open text editor";

  private final BooleanProperty useTextField = new SimpleBooleanProperty(true);
  public BooleanProperty useTextFieldProperty() { return useTextField; }
  public boolean isUseTextField() { return useTextField.get(); }
  public void setUseTextField(boolean useTextField) { this.useTextField.set(useTextField); }

  private final StringProperty windowTitle = new SimpleStringProperty();
  public StringProperty windowTitleProperty() { return windowTitle; }
  public void setWindowTitle(String windowTitle) { this.windowTitle.setValue(windowTitle); }
  public String getWindowTitle() { return this.windowTitle.getValue(); }

  public TextFieldArea() {
    this(null, false);
  }

  /** Construct of text field
   * @param windowTitle title of window if is open
   * @param useTextField use text field instead of text area */
  public TextFieldArea(String windowTitle, boolean useTextField) {
    super();
    getStyleClass().setAll(DEFAULT_STYLE_CLASS);
    this.useTextField.set(useTextField);
    this.windowTitle.set(windowTitle);
  }

  protected String buttonTooltip() {
    return BUTTON_TOOLTIP;
  }
  protected ImageView buttonImage() {
    return new ImageView(TextAreaFrmController.BUTTON_IMAGE);
  }
  protected EventHandler<ActionEvent> buttonEventHandler() {
    return TextAreaFrmController.openEventHandler(getWindowTitle(),
        this::getText, this::setText);
  }

  /** Text input control which edit value, and can be reimplement */
  protected TextInputControl createInputControl() {
    TextInputControl textInputControl = isUseTextField() ? new TextField() : new TextArea();
    if (!isUseTextField()) { textInputControl.setPrefHeight(Constants.TEXT_AREA_PREF_HIGH); }
    return textInputControl;
  }

  /*
  @Override
  protected Skin<?> createDefaultSkin() {
    return new TextFieldAreaSkin(this);
  }
  */

  /*
  public static class TextFieldAreaSkin extends SkinBase<TextFieldArea> {

    protected TextFieldAreaSkin(TextFieldArea textFieldArea) {
      super(textFieldArea);
      BorderPane pane = new BorderPane();
      pane.maxWidth(Double.MAX_VALUE);
      TextField tf = new TextField();
      tf.maxWidth(Double.MAX_VALUE);
      tf.setText("ahoj");
      tf.setEditable(true);
      pane.setCenter(tf);
      BorderPane.setAlignment(tf, Pos.CENTER_LEFT);
      */

      /*TextInputControl textInputControl = textFieldArea.isUseTextField() ? new TextField() : new TextArea();
      if (!textFieldArea.isUseTextField()) { textInputControl.setPrefHeight(Constants.TEXT_AREA_PREF_HIGH); }
      textInputControl.maxWidth(Double.MAX_VALUE);
      textInputControl.prefWidth(100.0);
      textInputControl.minWidth(100.0);
      */
      /*pane.setCenter(textInputControl);

      BorderPane.setAlignment(textInputControl, Pos.CENTER_LEFT);
      /*
      pane.setRight(TextAreaFrmController.createOpenButton(textFieldArea.getWindowTitle(),
          textInputControl::getText, textInputControl::setText));
          */
      // getChildren().add(pane);
      /*
      this.textInputControl.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
        switch (event.getCode()) {
          case ENTER:
          case TAB:
            textFieldArea.commitEdit(textInputControl.getText());
            break;
          case ESCAPE:
            textFieldArea.cancelEdit();
            break;
        }
      });
      this.textInputControl.focusedProperty().addListener((observable, oldValue, newValue) -> {
        if (!newValue) {
          if (textFieldArea.isCancelOnFocusLost()) { textFieldArea.cancelEdit(); }
          else { textFieldArea.commitEdit(textInputControl.getText()); }
        }
      });
      */
      // textFieldArea.editableProperty().addListener((observable, oldValue, newValue) -> textInputControl.editableProperty().setValue(newValue));
      // textFieldArea.textProperty().bindBidirectional(textFieldArea.textProperty());
      // textFieldArea.textProperty().addListener((observable, oldValue, newValue) -> textInputControl.textProperty().setValue(newValue));
    // }

    /** Commit given value to textProperty */
    /*
    public void commitEdit(String newValue) {
      getSkinnable().textProperty().setValue(newValue);
    }
    */

    /** If you call this method, then value in text field is set from the value, which was last commit or which was
     * set by setText() respectively to textProperty. */
    /*
    public void cancelEdit() {
      textInputControl.setText(text.getValue());
    }
    */
  //}
}
