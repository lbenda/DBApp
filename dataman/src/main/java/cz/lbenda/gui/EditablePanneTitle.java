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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This is component which can be insert as editable title of pane
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/18/14.
 */
public class EditablePanneTitle extends JPanel {

  private static final Logger LOG = LoggerFactory.getLogger(EditablePanneTitle.class);

  /** This listener is inform when title is changed */
  public interface OnTitleChangeListener {
    /** Method is called when user click on remove button */
    void onRemove(EditablePanneTitle title);
  }

  private final JLabel label = new JLabel();
  private final JButton bRemove = new JButton();

  private final List<OnTitleChangeListener> listeners = new ArrayList<>();

  public EditablePanneTitle(String text) {
    super();
    setText(text);
    configureRemoveButon();
    this.add(label);
    this.add(bRemove);
  }

  public final String getText() { return label.getText(); }
  public final void setText(String text) {
    LOG.debug("Text of editable pane: " + text);
    label.setText(text);
  }

  private void configureRemoveButon() {
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

  public void addOnTitleChangeListener(OnTitleChangeListener listener) {
    this.listeners.add(listener);
  }
}
