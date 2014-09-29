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
import cz.lbenda.dbapp.rc.db.Column;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import javax.swing.BorderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/29/14.
 */
public class DatePropertyEditor implements PropertyEditor {
  private static final Logger LOG = LoggerFactory.getLogger(DatePropertyEditor.class);
  public final Column column;
  private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
  private Date value;
  public DatePropertyEditor(final Column col) {
    this.column = col;
  }

  @Override
  public void setValue(Object value) {
    if (value instanceof String) {
      setAsText((String) value);
    } else { this.value = (Date) value; }
  }

  @Override
  public Object getValue() { return value; }

  @Override
  public boolean isPaintable() { return true; }
  @Override
  public void paintValue(Graphics gfx, Rectangle box) {
    JDateChooser dc = new JDateChooser();
    dc.setDate(value);
    dc.setBorder(BorderFactory.createEmptyBorder(0,3,0,0));
    dc.setForeground(Color.blue);
    dc.setBounds(box);
    dc.paint(gfx);
  }

  @Override
  public String getJavaInitializationString() { return "???"; }

  @Override
  public String getAsText() {
    if (value == null) { return null; }
    DateFormat formater = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
    return formater.format(value);
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (text == null || "".equals(text.trim())) { setValue(null); return; }
    DateFormat formater = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
    try {
      setValue(formater.parse(text));
    } catch (ParseException ex) {
      LOG.error("The given date isn't in right format: " + text, ex);
      throw new IllegalArgumentException("The given date isn't in right format: " + text, ex);
    }
  }

  @Override
  public String[] getTags() { return new String[0]; }

  @Override
  public Component getCustomEditor() {
    JDateChooser chooser = new JDateChooser();
    chooser.setDate(value);
    return chooser;
  }
  @Override
  public boolean supportsCustomEditor() { return true; }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) { pcs.addPropertyChangeListener(listener); }
  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) { pcs.removePropertyChangeListener(listener); }
}
