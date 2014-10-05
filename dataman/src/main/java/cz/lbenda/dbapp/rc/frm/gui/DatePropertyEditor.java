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

import com.toedter.calendar.JDateChooser;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.propertysheet.PropertyModel;
import org.openide.nodes.PropertyEditorRegistration;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Global editor for Date type
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/29/14.
 */
@Messages ({
    "DPE_DateNotSet=No Date Set",
    "DPE_Unparsablet=Could not parse date %s",
})
@PropertyEditorRegistration(targetType = Date.class)
public class DatePropertyEditor extends PropertyEditorSupport implements ExPropertyEditor, InplaceEditor.Factory {

  private static final Logger LOG = LoggerFactory.getLogger(DatePropertyEditor.class);

  private PropertyChangeListener listener = new PropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      LOG.trace("propertyChange: " + evt.getPropertyName() + " old value: " + evt.getOldValue() + " new value: " + evt.getNewValue());
      setValue(evt.getNewValue());
    }
  };

  @Override
  public String getAsText() {
    Date d = (Date) getValue();
    if (d == null) { return NbBundle.getMessage(DatePropertyEditor.class, "DPE_DateNotSet"); }
    DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
    return df.format(d);
  }

  @Override
  public void setAsText(String s) {
    try {
      DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
      setValue(df.parse(s));
    } catch (ParseException ps) {
      IllegalArgumentException iae = new IllegalArgumentException (String.format(NbBundle.getMessage(DatePropertyEditor.class, "DPE_Unparsable"), s));
      throw iae;
    }
  }

  @Override
  public void attachEnv(PropertyEnv env) {
    env.registerInplaceEditorFactory(this);
  }

  private InplaceEditor ed = null;

  @Override
  public InplaceEditor getInplaceEditor() {
    if (ed == null)  { ed = new Inplace(); }
    return ed;
  }

  private static class Inplace implements InplaceEditor {

    private final JDateChooser picker = new JDateChooser();
    private PropertyEditor editor = null;

    private PropertyChangeListener listener = new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        /*
        LOG.trace("propertyChange: " + evt.getPropertyName() + " old value: " + evt.getOldValue() + " new value: " + evt.getNewValue());
        if (getPropertyEditor() != null) {
          ((PropertyEditorSupport) getPropertyEditor()).firePropertyChange();
        }
        */
      }
    };

    public Inplace() {
      picker.addPropertyChangeListener(listener);
    }


    @Override
    public void connect(PropertyEditor propertyEditor, PropertyEnv env) {
      editor = propertyEditor;
      reset();
    }

    @Override
    public JComponent getComponent() {
      return picker;
    }

    @Override
    public void clear() {
      LOG.trace("clear");
      //avoid memory leaks:
      editor = null;
      model = null;
    }

    @Override
    public Object getValue() {
      return picker.getDate();
    }

    @Override
    public void setValue(Object object) {
      picker.setDate((Date) object);
    }

    @Override
    public boolean supportsTextEntry() {
      return true;
    }

    @Override
    public void reset() {
      Date d = (Date) editor.getValue();
      if (d != null) {
        picker.setDate(d);
      }
    }

    @Override
    public KeyStroke[] getKeyStrokes() {
      return new KeyStroke[0];
    }

    @Override
    public PropertyEditor getPropertyEditor() {
      return editor;
    }

    @Override
    public PropertyModel getPropertyModel() {
      return model;
    }

    private PropertyModel model;

    @Override
    public void setPropertyModel(PropertyModel propertyModel) {
      this.model = propertyModel;
    }

    @Override
    public boolean isKnownComponent(Component component) {
      return component == picker || picker.isAncestorOf(component);
    }

    @Override
    public void addActionListener(ActionListener actionListener) {
      //do nothing - not needed for this component
    }

    @Override
    public void removeActionListener(ActionListener actionListener) {
      //do nothing - not needed for this component
    }
  }
}
