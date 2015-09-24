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
package cz.lbenda.dataman.db.frm;

import cz.lbenda.dataman.db.ColumnDesc;
import cz.lbenda.dataman.db.RowDesc;
import javafx.beans.value.ObservableValue;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.editor.PropertyEditor;

import java.util.Optional;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 24.9.15. */
public class RowPropertyItem implements PropertySheet.Item {

  private ColumnDesc columnDesc;

  public ColumnDesc getColumnDesc() {
    return columnDesc;
  }

  private boolean editable;
  private RowDesc row;

  public RowPropertyItem(ColumnDesc columnDesc, RowDesc row, boolean editable) {
    this.columnDesc = columnDesc;
    this.editable = editable && row != null;
    this.row = row;
  }

  @Override
  public Class<?> getType() {
    return columnDesc.getDataType().getJavaClass();
  }

  @Override
  public String getCategory() { return null; }

  @Override
  public String getName() {
    return columnDesc.getName();
  }

  @Override
  public String getDescription() {
    return columnDesc.getLabel();
  }

  public ObservableValue valueProperty() {
    if (row == null) { return null; }
    return row.observableValueForColumn(columnDesc);
  }

  @Override
  public Object getValue() {
    if (row == null) { return null; }
    return row.getColumnValue(columnDesc);
  }
  @Override
  public void setValue(Object o) {
    if (row != null) { row.setColumnValue(columnDesc, o); }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Optional<Class<? extends PropertyEditor<?>>> getPropertyEditorClass() {
    return Optional.of((Class<? extends PropertyEditor<?>>) (Class) StringPropertyEditor.class);
  }

  @Override
  public boolean isEditable() {
    return editable;
  }
}
