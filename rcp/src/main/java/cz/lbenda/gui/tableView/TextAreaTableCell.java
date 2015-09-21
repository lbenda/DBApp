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
import cz.lbenda.rcp.DialogHelper;
import cz.lbenda.rcp.IconFactory;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Date;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 13.9.15.
 * Editing bigger texts in cell */
public class TextAreaTableCell<S> extends TableCell<S, String> {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(TextAreaTableCell.class);

  private final BorderPane panel = new BorderPane();
  private final TextInputControl textInputControl;
  private ObjectProperty<Callback<Integer, ObservableValue<LocalDate>>> selectedStateCallback;

  @SuppressWarnings("unchecked")
  /**  @param columnTitle title of column which is used as title of window with separate editor
   * @param useTextField Inform if is used text field or textInputControl for showing value in column */
  public static <S> Callback<TableColumn<S, LocalDate>, TableCell<S, LocalDate>> forTableColumn(String columnTitle, boolean useTextField) {
    return var2 -> new TextAreaTableCell(null, columnTitle, useTextField);
  }

  @SuppressWarnings("unchecked")
  public TextAreaTableCell(Callback<Integer, ObservableValue<LocalDate>> selectedStateCallback, String columnTitle, boolean useTextField) {
    this.textInputControl = useTextField ? new TextField() : new TextArea();
    this.selectedStateCallback = new SimpleObjectProperty(this, "selectedStateCallback");
    this.getStyleClass().add("text-area-table-cell");
    this.panel.setMaxWidth(Double.MAX_VALUE);
    this.panel.setPrefWidth(Double.MAX_VALUE);
    this.textInputControl.setMaxWidth(Double.MAX_VALUE);
    // this.textInputControl.setPrefWidth(Double.MAX_VALUE);

    this.panel.setCenter(textInputControl);
    BorderPane.setAlignment(textInputControl, Pos.TOP_LEFT);
    Button btOpenTextEditor = new Button(null, IconFactory.getInstance().imageView(this, "document-edit.png", IconFactory.IconLocation.TABLE_CELL));
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
    btOpenTextEditor.setOnAction(event -> {
      Tuple2<Parent, TextAreaFrmController> tuple2 = TextAreaFrmController.createNewInstance();
      tuple2.get2().textProperty().setValue(textInputControl.getText());
      tuple2.get2().textProperty().addListener((observable, oldValue, newValue) -> textInputControl.setText(newValue));
      DialogHelper.getInstance().openWindowInCenterOfStage((Stage) textInputControl.getScene().getWindow(),
          tuple2.get2().getMainPane(), columnTitle);
    });
    this.setGraphic(null);
    this.setSelectedStateCallback(selectedStateCallback);
  }

  public final ObjectProperty<Callback<Integer, ObservableValue<LocalDate>>> selectedStateCallbackProperty() {
    return this.selectedStateCallback;
  }

  public final void setSelectedStateCallback(Callback<Integer, ObservableValue<LocalDate>> callback) {
    this.selectedStateCallbackProperty().set(callback);
  }

  @SuppressWarnings({"unchecked", "unused"})
  public final Callback<Integer, ObservableValue<Date>> getSelectedStateCallback() {
    return (Callback) this.selectedStateCallbackProperty().get();
  }

  @Override
  public void startEdit() {
    if (this.isEditable() && this.getTableView().isEditable() && this.getTableColumn().isEditable()) {
      super.startEdit();
      if (this.isEditing()) {
        this.textInputControl.setText(getItem());
        this.setGraphic(panel);
        this.textInputControl.requestFocus();
        this.setText(null);
      }
    }
  }

  public void commitEdit(String newValue) {
    this.setItem(newValue);
    super.commitEdit(newValue);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void cancelEdit() {
    super.cancelEdit();
    textInputControl.setText(this.getItem());
    this.setText(getItem());
    this.setGraphic(null);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void updateItem(String value, boolean empty) {
    super.updateItem(value, empty);
    if (empty) {
      this.setText(null);
      this.setGraphic(null);
    } else {
      this.setText(value);
    }
  }
}

