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
package org.openide.explorer.view;

import cz.lbenda.dbapp.rc.db.TableDescription;
import org.netbeans.swing.outline.Outline;
import org.openide.nodes.Node;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.beans.PropertyChangeEvent;

/**
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/29/14.
 */
public class DateOutlineSheetCell extends SheetCell.OutlineSheetCell {

  private final Outline outline;
  private final TableDescription td;

  public DateOutlineSheetCell(Outline outline, TableDescription td) {
    super(outline);
    this.outline = outline;
    this.td = td;
  }

  @Override
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,int r, int c) {
    TableModel tableModel = outline.getModel();
    tableModel.addTableModelListener(this);
    return super.getTableCellEditorComponent(table, value, isSelected, r, c);
  }
}