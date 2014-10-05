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
package cz.lbenda.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This is component which can be insert as editable title of pane
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/18/14.
 */
public class EditablePanneTitle extends JPanel {

  private static final Logger LOG = LoggerFactory.getLogger(EditablePanneTitle.class);

  /** This listener is inform when title is changed */
  public interface OnTitleChangeListener {
    void onTitleChange(EditablePanneTitle tile, String text);
    /** Method is called when user click on remove button */
    void onRemove(EditablePanneTitle title);
  }

  private final JLabel label = new JLabel();
  private final JTextField tf = new JTextField();
  private final JButton bRemove = new JButton();

  private final List<OnTitleChangeListener> listeners = new ArrayList<>();

  private boolean editMode; public final boolean isEditMode() { return editMode; }

  public EditablePanneTitle() {
    setEditMode(false);
    setListeners();
    configureRemoveButon();
  }
  public EditablePanneTitle(String text) {
    setText(text);
    setEditMode(false);
    setListeners();
    configureRemoveButon();
  }

  public final String getText() { return label.getText(); }
  public final void setText(String text) {
    label.setText(text);
    tf.setText(text);
  }

  private final void configureRemoveButon() {
    bRemove.setBorder(BorderFactory.createEmptyBorder());
    bRemove.setContentAreaFilled(false);
    try {
      InputStream is = this.getClass().getResourceAsStream("/cz/lbenda/gui/icons/remove.png");
      BufferedImage buttonIcon = ImageIO.read(is);
      bRemove.removeAll();
      bRemove.setIcon(new ImageIcon(buttonIcon));
    } catch (Exception e) {
      // LOG.error("Problem with read icon for remove tabbed pane", e);
      bRemove.setText("X");
      bRemove.setFont(new Font("Berlin Sans FB",java.awt.Font.BOLD,14));
      bRemove.setForeground(new Color(255, 0, 0));
    }
    bRemove.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        if (e.getClickCount() == 2) {
          LOG.trace("The tabbed pane will be removed");
          for (OnTitleChangeListener listener : listeners) {
            listener.onRemove(EditablePanneTitle.this);
          }
        }
      }
    });
  }

  public final void setEditMode(boolean editMode) {
    this.editMode = editMode;
    if (editMode) {
      this.remove(label);
      this.remove(bRemove);
      this.add(tf);
      tf.requestFocus();
    } else {
      this.add(label);
      this.add(bRemove);
      this.remove(tf);
    }
  }

  private void titleChange(String text) {
    try {
      for (OnTitleChangeListener list : listeners) {
        list.onTitleChange(this, text);
      }
      label.setText(text);
      tf.setText(text);
    } catch (RuntimeException e) {
      LOG.error("The title can't be changed.", e);
      tf.setText(label.getText());
      throw e;
    }
  }

  private void setListeners() {
/*    this.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        super.mousePressed(e);

      }
    }); */
    label.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        if (e.getClickCount() == 2) { setEditMode(!editMode); }
        EditablePanneTitle.this.dispatchEvent(e);
      }
    });
    tf.addKeyListener(new KeyListener() {
      @Override public void keyTyped(KeyEvent e) { }
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getID() == KeyEvent.VK_ESCAPE) {
          setEditMode(!editMode);
          tf.setText(label.getText());
        } else if (e.getID() == KeyEvent.VK_ENTER) {
          setEditMode(!editMode);
          titleChange(tf.getText());
        }
      }
      @Override public void keyReleased(KeyEvent e) {}
    });
    tf.addFocusListener(new FocusListener() {
      @Override public void focusGained(FocusEvent e) {}
      @Override
      public void focusLost(FocusEvent e) {
        if (editMode) {
          titleChange(tf.getText());
          setEditMode(false);
        }
      }
    });
  }

  public void addOnTitleChangeListener(OnTitleChangeListener listener) {
    this.listeners.add(listener);
  }

  public void removeOnTitleChangeListener(OnTitleChangeListener listener) {
    this.listeners.remove(listener);
  }
}
