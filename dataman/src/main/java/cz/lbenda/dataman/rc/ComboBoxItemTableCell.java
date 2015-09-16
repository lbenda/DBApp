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
package cz.lbenda.dataman.rc;

import cz.lbenda.dataman.db.ComboBoxTDExtension;
import cz.lbenda.dataman.db.ComboBoxTDExtension.ComboBoxItem;
import cz.lbenda.dataman.db.RowDesc;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 14.9.15. */
public class ComboBoxItemTableCell extends TableCell<RowDesc, Object> {

  private static final Logger LOG = LoggerFactory.getLogger(ComboBoxItemTableCell.class);

  private final ObservableList<ComboBoxItem> items;
  private ComboBox<ComboBoxItem> comboBox;
  private ObjectProperty<StringConverter<Object>> converter;
  private ComboBoxTDExtension cbtde;

  public static Callback<TableColumn<RowDesc, Object>, TableCell<RowDesc, Object>> forTableColumn(@Nonnull ComboBoxTDExtension cbtde) {
    return (var2) -> new ComboBoxItemTableCell(cbtde);
  }

  public ComboBoxItemTableCell(@Nonnull ComboBoxTDExtension cbtde) {
    this.converter = new SimpleObjectProperty(this, "converter");
    this.cbtde = cbtde;
    this.getStyleClass().add("combo-box-table-cell");
    this.items = cbtde.getItems();
    this.setConverter(new StringConverter<Object>() {
      public String toString(Object value) {
        if (value == null) {
          return null;
        }
        ComboBoxItem cbItem = cbtde.itemForValue(value);
        if (cbItem == null) {
          LOG.warn("No choice for value '" + String.valueOf(value) + "' is defined.");
          return "No choice for value '" + String.valueOf(value) + "' is defined.";
        } else {
          return cbItem.getChoice();
        }
      }

      public Object fromString(String choice) {
        ComboBoxItem cbe = cbtde.itemForChoice(choice);
        if (cbe == null) {
          return null;
        }
        return cbe;
      }
    });
    this.itemProperty().addListener((observable, oldValue, newValue) -> {
      if (!isEditing()) {
        ComboBoxItem cbi = cbtde.itemForValue(newValue);
        if (cbi != null) {
          Tooltip tooltip = new Tooltip(cbi.getTooltip());
          this.setTooltip(tooltip);
        }
        setText(getConverter().toString(newValue));
      }
    });
    this.comboBox = new ComboBox<>(getItems());
    this.comboBox.editableProperty().setValue(false);
    this.comboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) { commitEdit(null); }
      else { commitEdit(newValue.getValue()); }
    });
    this.setGraphic(null);
  }

  public final ObjectProperty<StringConverter<Object>> converterProperty() {
    return this.converter;
  }

  public final void setConverter(StringConverter<Object> var1) {
    this.converterProperty().set(var1);
  }

  @SuppressWarnings("unchecked")
  public final StringConverter<Object> getConverter() {
    return (StringConverter) this.converterProperty().get();
  }

  public ObservableList<ComboBoxItem> getItems() {
    return this.items;
  }

  @Override
  public void startEdit() {
    if (this.isEditable() && this.getTableView().isEditable() && this.getTableColumn().isEditable()) {
      this.comboBox.getSelectionModel().select(cbtde.itemForValue(getItem()));
      super.startEdit();
      this.setText(null);
      this.setGraphic(this.comboBox);
    }
  }

  @Override
  public void commitEdit(Object value) {
    super.commitEdit(value);
    this.setItem(value);
    setGraphic(null);
  }

  @Override
  public void cancelEdit() {
    super.cancelEdit();
    this.setText(this.getConverter().toString(this.getItem()));
    this.setGraphic(null);
  }
}
