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

import cz.lbenda.common.StringConverters;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import jfxtras.scene.control.LocalTimeTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.LocalTime;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 13.9.15.
 * Create table column for editing date */
public class TimePickerTableCell<S, T> extends TableCell<S, T> {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(TimePickerTableCell.class);

  private final LocalTimeTextField localTimePicker;
  private ObjectProperty<StringConverter<T>> converter;
  private ObjectProperty<Callback<Integer, ObservableValue<LocalDateTime>>> selectedStateCallback;

  @SuppressWarnings("unchecked")
  public static <S> Callback<TableColumn<S, LocalDateTime>, TableCell<S, LocalDateTime>> forTableColumn() {
    return var2 -> new TimePickerTableCell(null, null);
  }

  @SuppressWarnings("unchecked")
  public TimePickerTableCell(Callback<Integer, ObservableValue<LocalDateTime>> selectedStateCallback, StringConverter<T> converter) {
    this.converter = new SimpleObjectProperty(this, "converter") {
      protected void invalidated() {}
    };
    this.selectedStateCallback = new SimpleObjectProperty(this, "selectedStateCallback");
    this.getStyleClass().add("date-time-picker-table-cell");
    this.setConverter(converter);
    this.localTimePicker = new LocalTimeTextField();
    this.localTimePicker.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      switch (event.getCode()) {
        case ENTER:
        case TAB:
          LocalTime ld = localTimePicker.getLocalTime();
          this.commitEdit((T) ld);
          break;
        case ESCAPE:
          this.cancelEdit();
          break;
      }
    });
    this.setGraphic(null);
    this.setSelectedStateCallback(selectedStateCallback);
  }

  public final ObjectProperty<StringConverter<T>> converterProperty() { return this.converter; }
  @SuppressWarnings("unchecked")
  public final void setConverter(StringConverter<T> var1) {
    if (var1 == null) { this.converterProperty().set((StringConverter<T>) StringConverters.LOCALTIME_CONVERTER); }
    else { this.converterProperty().set(var1); }
  }

  @SuppressWarnings("unchecked")
  public final StringConverter<T> getConverter() { return (StringConverter) this.converterProperty().get(); }

  public final ObjectProperty<Callback<Integer, ObservableValue<LocalDateTime>>> selectedStateCallbackProperty() {
    return this.selectedStateCallback;
  }

  public final void setSelectedStateCallback(Callback<Integer, ObservableValue<LocalDateTime>> callback) {
    this.selectedStateCallbackProperty().set(callback);
  }

  @Override
  public void startEdit() {
    if (this.isEditable() && this.getTableView().isEditable() && this.getTableColumn().isEditable()) {
      super.startEdit();
      if (this.isEditing()) {
        this.localTimePicker.setLocalTime((LocalTime) getItem());
        this.setGraphic(this.localTimePicker);
        this.localTimePicker.requestFocus();
        this.setText(null);
      }
    }
  }

  public void commitEdit(T newValue) {
    this.setItem(newValue);
    super.commitEdit(newValue);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void cancelEdit() {
    super.cancelEdit();
    this.setText(this.getConverter().toString(getItem()));
    this.setGraphic(null);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void updateItem(T value, boolean empty) {
    super.updateItem(value, empty);
    if (empty) {
      this.setText(null);
      this.setGraphic(null);
    } else {
      setText(getConverter().toString(getItem()));
      setGraphic(null);
    }
  }
}
