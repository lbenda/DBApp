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
package cz.lbenda.rcp.tableView;

import cz.lbenda.common.StringConverters;
import cz.lbenda.rcp.localization.MessageFactory;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Date;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 13.9.15.
 * Create table column for editing date */
public class DatePickerTableCell<S, T> extends TableCell<S, T> {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(DatePickerTableCell.class);

  private final DatePicker datePicker;
  private ObservableValue dateProperty;
  private ObjectProperty<StringConverter<T>> converter;
  private ObjectProperty<Callback<Integer, ObservableValue<LocalDate>>> selectedStateCallback;

  @SuppressWarnings("unchecked")
  public static <S> Callback<TableColumn<S, LocalDate>, TableCell<S, LocalDate>> forTableColumn() {
    return var2 -> new DatePickerTableCell(null, null);
  }

  @SuppressWarnings("unchecked")
  public DatePickerTableCell(Callback<Integer, ObservableValue<LocalDate>> selectedStateCallback, StringConverter<T> converter) {
    MessageFactory.initializeMessages(this);
    this.converter = new SimpleObjectProperty(this, "converter") {
      protected void invalidated() {}
    };
    this.selectedStateCallback = new SimpleObjectProperty(this, "selectedStateCallback");
    this.getStyleClass().add("date-picker-table-cell");
    this.setConverter(converter);
    this.datePicker = new DatePicker();
    this.datePicker.setConverter(StringConverters.LOCALDATE_CONVERTER);
    this.datePicker.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      switch (event.getCode()) {
        case ENTER:
        case TAB:
          LocalDate ld = datePicker.getConverter().fromString(datePicker.editorProperty().getValue().getText());
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
    if (var1 == null) { this.converterProperty().set((StringConverter<T>) StringConverters.LOCALDATE_CONVERTER); }
    else { this.converterProperty().set(var1); }
  }

  @SuppressWarnings("unchecked")
  public final StringConverter<T> getConverter() { return (StringConverter) this.converterProperty().get(); }

  public final ObjectProperty<Callback<Integer, ObservableValue<LocalDate>>> selectedStateCallbackProperty() {
    return this.selectedStateCallback;
  }

  public final void setSelectedStateCallback(Callback<Integer, ObservableValue<LocalDate>> callback) {
    this.selectedStateCallbackProperty().set(callback);
  }

  @SuppressWarnings("unchecked")
  public final Callback<Integer, ObservableValue<Date>> getSelectedStateCallback() {
    return (Callback) this.selectedStateCallbackProperty().get();
  }

  @Override
  public void startEdit() {
    if (this.isEditable() && this.getTableView().isEditable() && this.getTableColumn().isEditable()) {
      super.startEdit();
      if (this.isEditing()) {
        this.datePicker.setValue((LocalDate) getItem());
        this.setGraphic(this.datePicker);
        this.datePicker.requestFocus();
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
    this.setItem((T) this.datePicker.getValue());
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
    } else if (isEditing()) {
      this.setGraphic(this.datePicker);
      if (this.dateProperty instanceof ObjectProperty) {
        this.datePicker.valueProperty().unbindBidirectional((ObjectProperty) this.dateProperty);
      }

      ObservableValue selected = this.getSelectedProperty();
      if (selected instanceof ObjectProperty) {
        this.dateProperty = selected;
        this.datePicker.valueProperty().bindBidirectional((ObjectProperty) selected);
      }

      this.datePicker.disableProperty().bind(Bindings.not(this.getTableView().editableProperty().and(this.getTableColumn().editableProperty()).and(this.editableProperty())));
    } else {
      setText(getConverter().toString(getItem()));
      setGraphic(null);
    }
  }

  private ObservableValue<?> getSelectedProperty() {
    return this.getSelectedStateCallback() != null ? (ObservableValue) this.getSelectedStateCallback().call(this.getIndex())
        : this.getTableColumn().getCellObservableValue(this.getIndex());
  }
}

