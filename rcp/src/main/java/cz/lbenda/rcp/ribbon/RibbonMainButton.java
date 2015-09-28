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
package cz.lbenda.rcp.ribbon;

import cz.lbenda.rcp.action.Action;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.MessageFactory;
import cz.lbenda.rcp.ribbon.skin.RibbonMainButtonSkin;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.image.Image;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 24.9.15.
 * Controller for button which is show on left top corner of window */
public class RibbonMainButton extends Control {
  public final static String DEFAULT_STYLE_CLASS = "main-button";

  private ContextMenu menu = new ContextMenu();
  private String appName; public String getAppName() { return appName; }
  private Image appImg; public Image getAppImg() { return appImg; }

  /** Create main ribbon button */
  public RibbonMainButton(String appName, Image appImg, Ribbon ribbon) {
    getStyleClass().setAll(DEFAULT_STYLE_CLASS);
    this.appName = appName;
    this.appImg = appImg;
    ribbon.itemsProperty().addListener((ListChangeListener<Object>) change -> {
      while (change.next()) {
        if (change.wasAdded()) { change.getAddedSubList().forEach(this::addMenuItem);  }
        // TODO removed
      }
    });
  }

  private void addMenuItem(Object item) {
    if (item instanceof EventHandler) { addEventHandlerToMenu((Action) item); }
    // TODO combo box / options
  }

  private void addEventHandlerToMenu(Action event) {
    ActionConfig ac = event.getClass().getAnnotation(ActionConfig.class);
    if (!ac.showInRibbon()) {
      ActionGUIConfig gui = ac.gui()[0];
      MenuItem item = new MenuItem(MessageFactory.getInstance().getMessage(gui.displayName()));
      menu.getItems().add(item);
      item.setOnAction(event);
    }
  }

  @Override
  protected Skin<?> createDefaultSkin() {
    return new RibbonMainButtonSkin(this);
  }

  public ContextMenu getMenu() {
    return menu;
  }
}
