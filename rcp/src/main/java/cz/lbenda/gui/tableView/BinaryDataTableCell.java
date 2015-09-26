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

import cz.lbenda.common.*;
import cz.lbenda.gui.controls.BinaryDataEditor;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 13.9.15.
 * Editing bigger texts in cell */
public class BinaryDataTableCell<S> extends TableCell<S, BinaryData> {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(BinaryDataTableCell.class);

  private final BinaryDataEditor binaryDataEditor;

  @SuppressWarnings("unchecked")
  /** Create for table column
   * @param textual inform if values in items have textual or binary character */
  public static <S> Callback<TableColumn<S, BinaryData>, TableCell<S, BinaryData>> forTableColumn(boolean textual) {
    return var2 -> new BinaryDataTableCell(textual);
  }

  @SuppressWarnings("unchecked")
  public BinaryDataTableCell(boolean textual) {
    binaryDataEditor = new BinaryDataEditor(textual ? ClobBinaryData.NULL : BlobBinaryData.NULL, isEditable());
    editableProperty().addListener((observable, oldValue, newValue) -> binaryDataEditor.setEditable(newValue));
    this.getStyleClass().add("text-area-table-cell");
    this.setGraphic(null);
    binaryDataEditor.binaryDataProperty().addListener((observable, oldValue, newValue) -> {
      commitEdit(newValue);
    });
  }

  @Override
  public void startEdit() {
    if (this.isEditable() && this.getTableView().isEditable() && this.getTableColumn().isEditable()) {
      super.startEdit();
      if (this.isEditing()) {
        this.binaryDataEditor.setBinaryData(getItem());
        this.setGraphic(binaryDataEditor);
        this.binaryDataEditor.requestFocus();
        this.setText(null);
      }
    }
  }

  public void commitEdit(BinaryData newValue) {
    if (!AbstractHelper.nullEquals(newValue, getItem())) {
      binaryDataEditor.setBinaryData(newValue);
      super.commitEdit(newValue);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void cancelEdit() {
    super.cancelEdit();
    binaryDataEditor.setBinaryData(getItem());
    this.setText(StringConverters.BINARYDATA_CONVERTER.toString(getItem()));
    this.setGraphic(null);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void updateItem(BinaryData value, boolean empty) {
    super.updateItem(value, empty);
    if (empty) {
      this.setText(null);
      this.setGraphic(null);
    } else {
      this.setText(StringConverters.BINARYDATA_CONVERTER.toString(value));
    }
  }
}

