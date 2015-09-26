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

import cz.lbenda.common.AbstractHelper;
import cz.lbenda.gui.controls.TextFieldArea;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
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

  private final TextFieldArea textFieldArea;
  private ObjectProperty<Callback<Integer, ObservableValue<LocalDate>>> selectedStateCallback;

  @SuppressWarnings("unchecked")
  /** @param columnTitle title of column which is used as title of window with separate editor
   * @param useTextField Inform if is used text field or textInputControl for showing value in column */
  public static <S> Callback<TableColumn<S, LocalDate>, TableCell<S, LocalDate>> forTableColumn(String columnTitle,
                                                                                                boolean useTextField) {
    return var2 -> new TextAreaTableCell(null, columnTitle, useTextField);
  }

  @SuppressWarnings("unchecked")
  public TextAreaTableCell(Callback<Integer, ObservableValue<LocalDate>> selectedStateCallback,
                           String columnTitle,
                           boolean useTextField) {
    textFieldArea = new TextFieldArea(columnTitle, useTextField);
    this.selectedStateCallback = new SimpleObjectProperty(this, "selectedStateCallback");
    this.getStyleClass().add("text-area-table-cell");
    this.setGraphic(null);
    this.setSelectedStateCallback(selectedStateCallback);
    textFieldArea.textProperty().addListener((observable, oldValue, newValue) -> {
      commitEdit(newValue);
    });
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
        this.textFieldArea.setText(getItem());
        this.setGraphic(textFieldArea.getNode());
        this.textFieldArea.requestFocus();
        this.setText(null);
      }
    }
  }

  public void commitEdit(String newValue) {
    if (!AbstractHelper.nullEquals(newValue, getItem())) {
      textFieldArea.commitEdit(newValue);
      super.commitEdit(newValue);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void cancelEdit() {
    super.cancelEdit();
    textFieldArea.cancelEdit();
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

