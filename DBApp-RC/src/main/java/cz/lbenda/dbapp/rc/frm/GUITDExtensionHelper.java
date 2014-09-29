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
package cz.lbenda.dbapp.rc.frm;

import com.toedter.calendar.JDateChooser;
import cz.lbenda.dbapp.rc.db.Column;
import cz.lbenda.dbapp.rc.db.ComboBoxTDExtension;
import cz.lbenda.dbapp.rc.db.TableDescriptionExtension;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Helper class which create GUI elements for helper
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/26/14.
 */
public abstract  class GUITDExtensionHelper {

  public enum ColumnEditType {
    TextField, DateChooser, ComboBox
  }

  public static ColumnEditType editTypeForColumn(final Column column) {
    List<TableDescriptionExtension> tde = column.getExtensions();
    if (tde.isEmpty()) {
      switch (column.getDataType()) {
        case DATE : return ColumnEditType.DateChooser;
        case STRING :
        default : return ColumnEditType.TextField;
      }
    } else {
      return ColumnEditType.ComboBox;
    }
  }

  /** Return edit component for column
   * @param column column for which is edit element created
   * @return edit component for given column
   */
  public static JComponent editComponent(final Column column) {
    switch (editTypeForColumn(column)) {
      case DateChooser: return new JDateChooser();
      case ComboBox: return editComponent(column.getExtensions().get(0)); // FIXME more then one extension can be on column
      case TextField:
      default:
        return new JTextField();
    }
  }

  /** Return edit component for table extension
   * @param tde extended description
   */
  public static JComponent editComponent(final TableDescriptionExtension tde) {
    if (tde instanceof ComboBoxTDExtension) {
      ComboBoxTDExtension sbe = (ComboBoxTDExtension) tde;
      JComboBox<ComboBoxTDExtension.ComboBoxItem> cb = new JComboBox<>();
      cb.setModel(new TOKComboBoxModel(sbe));
      return cb;
    }
    return null;
  }

  /** This method set to component value which is on row
   * @param value values of row
   * @param comp component to which is data set
   * @param column column which is configured
   */
  @SuppressWarnings("unchecked")
  public static void componentValue(Object value, final JComponent comp, final Column column) {
    switch (editTypeForColumn(column)) {
      case ComboBox:
        TOKComboBoxModel cbm = (TOKComboBoxModel) ((JComboBox<ComboBoxTDExtension.ComboBoxItem>) comp).getModel();
        cbm.setSelectedValue(value);
        break;
      case DateChooser: ((JDateChooser) comp).setDate((Date) value); break;
      case TextField:
      default: ((JTextField) comp).setText(value != null ? String.valueOf(value) : "");
    }
  }

  /** This method set to component value which is on row
   * @param values values of row
   * @param comp component to which is data set
   * @param column column which is configured
   */
  @SuppressWarnings("unchecked")
  public static void componentValue(final Map<Column, Object> values, final JComponent comp, final Column column) {
    componentValue(values.get(column), comp, column);
  }

  public static Object valueInComponent(final JComponent comp, final Column column) {
    switch (editTypeForColumn(column)) {
      case ComboBox:
        return ((ComboBoxTDExtension.ComboBoxItem) ((JComboBox<ComboBoxTDExtension.ComboBoxItem>) comp)
            .getSelectedItem()).getValue();
      case DateChooser: return ((JDateChooser) comp).getDate();
      case TextField:
      default: return ((JTextField) comp).getText();
    }
  }

  @SuppressWarnings("unchecked")
  public static void componentToValues(final Map<Column, Object> values, final JComponent comp, final Column column) {
    values.put(column, valueInComponent(comp, column));
  }

  public static class TOKTableCellEditor implements TableCellEditor {
    private final Column column;
    private Set<CellEditorListener> listeners = new HashSet<>();
    private JComponent comboBox;
    public TOKTableCellEditor(Column column) {
      this.column = column;
    }
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      comboBox = GUITDExtensionHelper.editComponent(this.column);
      GUITDExtensionHelper.componentValue(value, comboBox, this.column);
      return comboBox;
    }

    @Override
    public Object getCellEditorValue() {
      return GUITDExtensionHelper.valueInComponent(comboBox, this.column);
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) { return true; }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) { return true; }

    @Override
    public boolean stopCellEditing() {
      return false;
    }

    @Override
    public void cancelCellEditing() {

    }

    @Override
    public void addCellEditorListener(CellEditorListener l) { listeners.add(l); }
    @Override
    public void removeCellEditorListener(CellEditorListener l) { listeners.remove(l); }
  }

  private static class TOKComboBoxModel implements ComboBoxModel<ComboBoxTDExtension.ComboBoxItem> {

    private final List<ListDataListener> listeners = new ArrayList<>();
    private final ComboBoxTDExtension extension;
    private ComboBoxTDExtension.ComboBoxItem selectedItem;

    public TOKComboBoxModel(final ComboBoxTDExtension extension) {
      this.extension = extension;
    }

    public final void setSelectedValue(final Object value) {
      for (ComboBoxTDExtension.ComboBoxItem item : extension.getItems()) {
        if (value == null) { setSelectedItem(ComboBoxTDExtension.ComboBoxItem.EMPTY); }
        else if (value.equals(item.getValue())) { setSelectedItem(item); }
      }
    }

    @Override
    public final void setSelectedItem(Object anItem) {
      ComboBoxTDExtension.ComboBoxItem old = selectedItem;
      selectedItem = (ComboBoxTDExtension.ComboBoxItem) anItem;
      for (ListDataListener l : listeners) {
        int index = this.extension.getItems().indexOf(old);
        if (index != -1) { l.intervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index)); }
        if (anItem != null) {
          index = this.extension.getItems().indexOf(anItem);
          if (index == -1) { index = 0; }
          l.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index, index));
        }
      }
    }


    @Override
    public final Object getSelectedItem() { return selectedItem; }

    @Override
    public final int getSize() { return extension.getItems().size() + 1; }
    @Override
    public final ComboBoxTDExtension.ComboBoxItem getElementAt(int index) {
      if (index == 0) { return ComboBoxTDExtension.ComboBoxItem.EMPTY; }
      return extension.getItems().get(index - 1);
    }

    @Override
    public final void addListDataListener(ListDataListener l) { listeners.add(l); }
    @Override
    public final void removeListDataListener(ListDataListener l) { listeners.remove(l); }
  }
}
