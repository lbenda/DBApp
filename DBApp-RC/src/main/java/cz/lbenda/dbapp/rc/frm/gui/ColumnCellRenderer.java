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
package cz.lbenda.dbapp.rc.frm.gui;

import cz.lbenda.dbapp.rc.db.Column;
import cz.lbenda.dbapp.rc.db.TableDescription;
import org.netbeans.swing.outline.DefaultOutlineCellRenderer;
import org.openide.awt.HtmlRenderer;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/** Column cell renderer for Outline - remove three points buttons.
* Created by Lukas Benda <lbenda @ lbenda.cz> on 10/1/14.
*/
public class ColumnCellRenderer extends DefaultOutlineCellRenderer {
  /**
   * Gray Color for the odd lines in the view.
   */
  private static final Color VERY_LIGHT_GRAY = new Color(236, 236, 236);
  /**
   * Center the content of the cells displaying text.
   */
  protected boolean centered = System.getProperty("os.name").toLowerCase().indexOf("windows") < 0;
  /**
   * Highlight the non editable cells making the foreground lighter.
   */
  protected boolean lighterEditableFields = false;
  private final List<Column> columns;

  public ColumnCellRenderer(List<Column> columns) {
    this.columns = columns;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Component getTableCellRendererComponent(final JTable table,
                                                 final Object value,
                                                 final boolean isSelected,
                                                 final boolean hasFocus,
                                                 final int row,
                                                 final int column) {
    final Component cell;
    Object valueToDisplay = value;
    Class valueClasss = columns.get(column - 1).getDataType().getDataType(); // First column is NODE
    if (value instanceof Node.Property) {
      try {
        valueToDisplay = ((Node.Property) value).getValue();
      } catch (IllegalAccessException ex) {
        Exceptions.printStackTrace(ex);
      } catch (InvocationTargetException ex) {
        Exceptions.printStackTrace(ex);
      }
    }
    TableCellRenderer renderer = table.getDefaultRenderer(valueClasss);
    if (renderer != null) {
      cell = renderer.getTableCellRendererComponent(table, valueToDisplay, isSelected,
          hasFocus, row, column);
    } else {
      cell = super.getTableCellRendererComponent(table, valueToDisplay, isSelected, hasFocus, row, column);
    }
    if (cell != null) {
      if (centered) {
        if (cell instanceof HtmlRenderer.Renderer) {
          ((HtmlRenderer.Renderer) cell).setCentered(centered);
        } else if (cell instanceof UIResource) {
          ((UIResource) cell).setHorizontalAlignment(JLabel.CENTER);
        }
      }
      Color foregroundColor = table.getForeground();
      int modelRow = table.convertRowIndexToModel(row);
      int modelColumn = table.convertColumnIndexToModel(column);
      final boolean cellEditable = table.getModel().isCellEditable(modelRow, modelColumn);
      if (lighterEditableFields && cellEditable) {
        foregroundColor = Color.BLUE;
      }
      cell.setForeground(foregroundColor);
      cell.setBackground(row % 2 == 0 ? Color.WHITE : VERY_LIGHT_GRAY);
      if (isSelected) {
        if (lighterEditableFields && cellEditable) {
          cell.setFont(cell.getFont().deriveFont(Font.BOLD));
        }
        cell.setBackground(table.getSelectionBackground());
      }
    }
    return cell;
  }

  /**
   * @return true if the text rendered in labels is centered.
   */
  public boolean isCentered() {
    return centered;
  }
  public void setCentered(boolean centered) { this.centered = centered; }

  /**
   * @return true if non editable cells have a lighter foreground.
   */
  public boolean isLighterEditableFields() {
    return lighterEditableFields;
  }

  /**
   * Highlight the non editable cells making the foreground lighter.
   *
   * @param value true to activate this feature.
   */
  public void setLighterEditableFields(final boolean value) {
    this.lighterEditableFields = value;
  }
}
