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

import cz.lbenda.dbapp.rc.AbstractHelper;
import cz.lbenda.dbapp.rc.db.Column;
import cz.lbenda.dbapp.rc.db.ComboBoxTDExtension;
import cz.lbenda.dbapp.rc.db.TableDescription;
import cz.lbenda.dbapp.rc.frm.gui.TOKPropertyEditor;
import java.awt.Component;
import java.awt.datatransfer.Transferable;
import java.beans.IntrospectionException;
import java.beans.PropertyEditor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.swing.Action;
import org.openide.actions.OpenAction;
import org.openide.cookies.OpenCookie;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.NewType;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Representation of row in table
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/28/14.
 */
public class RowNode extends AbstractNode {

  private final static Logger LOG = LoggerFactory.getLogger(RowNode.class);

  private final Row row;

  public RowNode(Row row) throws IntrospectionException {
    this(row, new InstanceContent());
  }

  public RowNode(final Row row, final InstanceContent ic) throws IntrospectionException {
    super(Children.LEAF, new AbstractLookup(ic));
    this.row = row;
    ic.add(new OpenCookie() {
      @Override
      public void open() {
        ChosenTable.getInstance().setSelectedRowValues(row);
      }
    });
  }

  @Override
  public Action getPreferredAction() {
    return SystemAction.get(OpenAction.class);
  }
  @Override
  public Action[] getActions(boolean context) {
    return new Action[]{SystemAction.get(OpenAction.class)};
  }

  @Override
  public Node cloneNode() {
    try {
      return new RowNode(row);
    } catch (IntrospectionException e) {
      LOG.error("Problem with cloning node");
      throw new RuntimeException("Problem with cloning node", e);
    }
  }

  @Override
  public HelpCtx getHelpCtx() {
    return null;
  }

  @Override
  public boolean canRename() {
    return false;
  }

  @Override
  public boolean canDestroy() {
    return false;
  }

  @Override
  public PropertySet[] getPropertySets() {
    PropertySet[] result = new PropertySet[row.getTableDescription().getColumns().size()];
    int i = 0;
    for (Column col : row.getTableDescription().getColumns()) {
      result[i] = new ColumnProperty(col, row);
      i++;
    }
    return result;
  }

  @Override
  public Transferable clipboardCopy() throws IOException {
    return null;
  }

  @Override
  public Transferable clipboardCut() throws IOException {
    return null;
  }

  @Override
  public Transferable drag() throws IOException {
    return null;
  }

  @Override
  public boolean canCopy() {
    return false;
  }

  @Override
  public boolean canCut() {
    return false;
  }

  // TODO createPastType

  @Override
  public PasteType getDropType(Transferable t, int action, int index) {
    return null;
  }

  @Override
  public NewType[] getNewTypes() {
    return new NewType[0];
  }

  @Override
  public boolean hasCustomizer() { return false; }

  @Override
  public Component getCustomizer() {
    return null;
  }

  @Override
  public Handle getHandle() {
    return null;
  }

  public class ColumnProperty extends PropertySet {
    private final Column column;
    private final Row row;
    public ColumnProperty(Column column, Row row) {
      super(column.getName(), column.getName(), column.getName());
      this.column = column;
      this.row = row;
    }
    @Override
    public Property<?>[] getProperties() {
      return new Property<?>[] { new ColumnRowProperty(column, row) };
    }
  }

  public static class Row {
    private final TableDescription tableDescription; public final TableDescription getTableDescription() { return tableDescription; }
    private final Object[] rowValues; public final Object[] getRowValues() { return rowValues; }

    public Row(TableDescription td) {
      this.tableDescription = td;
      this.rowValues = new Object[td.columnCount()];
    }

    public Row(TableDescription td, Object[] rowValues) {
      this.tableDescription = td;
      this.rowValues = rowValues;
    }

    public final Object getValue(Column column) {
      return rowValues[column.getPosition()];
    }
    public final Object getValue(int position) {
      return rowValues[position];
    }
    public final void setValue(Column column, Object value) {
      rowValues[column.getPosition()] = value;
    }
    public final void setValue(int position, Object value) {
      rowValues[position] = value;
    }

    public final Object getId() {
      java.util.List<Column> pks = tableDescription.getPKColumns();
      if (pks.isEmpty()) { return "Empty"; }
      return rowValues[pks.get(0).getPosition()];
    }
  }

  public class ColumnRowProperty extends Property<Object> {
    private final Column column;
    private final Row row;
    private final Object orignalValue;

    public ColumnRowProperty(Column column, Row row) {
      super(column.getDataType().getDataType());
      this.setName(column.getName());
      this.column = column;
      this.row = row;
      orignalValue = row.getRowValues()[column.getPosition()];
    }
    @Override
    public boolean canRead() { return true; }

    @Override
    public Object getValue() throws IllegalAccessException, InvocationTargetException {
      if (column.getExtensions().isEmpty()) {
        /* if (column.getDataType() == Column.ColumnType.DATE) {
          Date value = (Date) row.getRowValues()[column.getPosition()];
          if (value == null) { return null; }
          DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
          return format.format(value);
        } else { */ return row.getRowValues()[column.getPosition()];// }
      }

      ComboBoxTDExtension cb = (ComboBoxTDExtension) column.getExtensions().get(0); // FIXME
      ComboBoxTDExtension.ComboBoxItem item = cb.itemForValue(row.getRowValues()[column.getPosition()]);
      if (item == null) { return null; }
      return cb.itemForValue(row.getRowValues()[column.getPosition()]).getChoice();
    }

    public boolean supportsDefaultValue() { return true; }
    public void restoreDefaultValue() throws IllegalAccessException, InvocationTargetException {
      setValue(orignalValue);
    }
    public boolean isDefaultValue() {
      return AbstractHelper.nullEquals(orignalValue, row.getRowValues()[column.getPosition()]);
    }


    @Override
    public void setValue(Object val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      if (column.getExtensions().isEmpty()) {
        /*if (column.getDataType() == Column.ColumnType.DATE) {
          Date date = null;
          if (val instanceof Date) {
            date = (Date) val;
          } else if (val != null) {
            DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
            try {
              date = format.parse((String) val);
            } catch (ParseException ex) {
              LOG.error("Problem with parse Date string: " + val, ex);
              throw new IllegalArgumentException("Problem with parse Date string: " + val, ex);
            }
          }
          row.getRowValues()[column.getPosition()] = date;
        } else {*/ row.getRowValues()[column.getPosition()] = val; //}
      } else {
        ComboBoxTDExtension cb = (ComboBoxTDExtension) column.getExtensions().get(0); // FIXME
        ComboBoxTDExtension.ComboBoxItem item = cb.itemForChoice((String) val);
        row.getRowValues()[column.getPosition()] = item == null ? null : item.getValue();
      }
      LOG.trace("before fire");
      try {
        firePropertyChange(column.getName(), orignalValue, val);
      } catch (Exception e) {
        LOG.error("Error when property is fired.", e);
      }
      LOG.trace("after fire");
    }

    @Override
    public boolean canWrite() {
      return row.getTableDescription().getTableType() == TableDescription.TableType.TABLE;
    }

    @Override
    public PropertyEditor getPropertyEditor() {
      if (column.getExtensions().isEmpty()) {
        // if (column.getDataType() == Column.ColumnType.DATE) {
        //  return new DatePropertyEditor(column);
        /* } else { */ return super.getPropertyEditor(); //}
      }
      return new TOKPropertyEditor(column);
    }
  }
}
