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
import cz.lbenda.dbapp.rc.db.ComboBoxTDExtension;
import cz.lbenda.dbapp.rc.db.TableDescriptionExtension;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/29/14.
 */
public class TOKPropertyEditor implements PropertyEditor {

  private final static Logger LOG = LoggerFactory.getLogger(TOKPropertyEditor.class);

  private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
  private final Column column;
  private Object value;

  public TOKPropertyEditor(final Column column) {
    this.column = column;
  }

  @Override
  public void setValue(Object value) {
    Object oldValue = this.value;
    this.value = value;
    pcs.firePropertyChange(column.getName(), oldValue, value);
  }

  @Override
  public Object getValue() { return value; }

  @Override
  public boolean isPaintable() { return false; }
  @Override
  public void paintValue(Graphics gfx, Rectangle box) {
  }

  @Override
  public String getJavaInitializationString() { return "???"; }

  @Override
  public String getAsText() {
    /* if (value == null) { return null; }
    java.util.List<TableDescriptionExtension> tdes = column.getExtensions();
    ComboBoxTDExtension ext = (ComboBoxTDExtension) tdes.get(0);
    for (ComboBoxTDExtension.ComboBoxItem cbi : ext.getItems()) {
      if (value.equals(cbi.getValue())) { return cbi.getChoice(); }
    }*/
    return (String) value;
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    /* if (text == null) { setValue(null); }
    else {
      java.util.List<TableDescriptionExtension> tdes = column.getExtensions();
      ComboBoxTDExtension ext = (ComboBoxTDExtension) tdes.get(0);
      for (ComboBoxTDExtension.ComboBoxItem cbi : ext.getItems()) {
        if (text.equals(cbi.getChoice())) {
          setValue(cbi.getValue());
          return;
        }
      }
    }*/
    setValue(text);
  }

  @Override
  public String[] getTags() {
    java.util.List<TableDescriptionExtension> tdes = column.getExtensions();
    ComboBoxTDExtension ext = (ComboBoxTDExtension) tdes.get(0);
    String[] result = new String[ext.getItems().size()];
    int i = 0;
    for (ComboBoxTDExtension.ComboBoxItem cbi : ext.getItems()) {
      result[i] = cbi.getChoice();
      i++;
    }
    return result;
  }

  @Override
  public Component getCustomEditor() { return null; } // return GUITDExtensionHelper.editComponent(column); }
  @Override
  public boolean supportsCustomEditor() { return false; }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) { pcs.addPropertyChangeListener(listener); }
  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) { pcs.removePropertyChangeListener(listener); }
}
