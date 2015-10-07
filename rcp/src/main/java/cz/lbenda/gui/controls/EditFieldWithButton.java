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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 5.10.15.
 * Edit field which contains also button with function */
public abstract class EditFieldWithButton extends Control {

  /** Property which set component to editable, or not editable mode */
  private final BooleanProperty editable = new SimpleBooleanProperty(true);
  /** Property which hold actual value of component. No value in text field, but value which was commit or set by setText. */
  private final StringProperty text = new SimpleStringProperty();

  public EditFieldWithButton() {
    super();
  }

  @Override
  protected Skin<?> createDefaultSkin() {
    return new EditFieldWithButtonSkin(this);
  }

  public boolean isEditable() { return editable.get(); }
  public void setEditable(boolean editable) { this.editable.setValue(editable); }
  @SuppressWarnings("unused")
  public BooleanProperty editableProperty() { return editable; }

  public String getText() { return text.getValue(); }
  public void setText(String value) { this.text.setValue(value); }
  public StringProperty textProperty() { return text; }

  protected abstract String buttonTooltip();
  protected abstract ImageView buttonImage();
  protected abstract EventHandler<ActionEvent> buttonEventHandler();

  /** Text input control which edit value, and can be reimplement */
  protected TextInputControl createInputControl() { return new TextField(); }

  public static class EditFieldWithButtonSkin extends SkinBase<EditFieldWithButton> {

    protected EditFieldWithButtonSkin(EditFieldWithButton editFieldWithButton) {
      super(editFieldWithButton);
      BorderPane pane = new BorderPane();
      pane.setMaxWidth(Double.MAX_VALUE);
      TextInputControl tf = editFieldWithButton.createInputControl();
      tf.maxWidth(Double.MAX_VALUE);
      tf.setText(editFieldWithButton.getText());
      tf.setEditable(editFieldWithButton.isEditable());
      Button button = new Button();
      button.setDisable(!editFieldWithButton.isEditable());
      button.setTooltip(new Tooltip(editFieldWithButton.buttonTooltip()));
      button.setGraphic(editFieldWithButton.buttonImage());
      pane.setCenter(tf);
      BorderPane.setAlignment(tf, Pos.CENTER_LEFT);
      pane.setRight(button);
      getChildren().add(pane);

      tf.textProperty().bindBidirectional(editFieldWithButton.textProperty());

      button.setOnAction(editFieldWithButton.buttonEventHandler());
    }
  }
}
