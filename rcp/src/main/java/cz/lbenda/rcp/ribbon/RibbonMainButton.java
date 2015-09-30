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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 24.9.15.
 * Controller for button which is show on left top corner of window */
public class RibbonMainButton extends Control {

  public final static String MENU_CATEGORY = "menuCategory_/";
  public final static String DEFAULT_STYLE_CLASS = "main-button";

  private Map<String, MenuItem> category = new ConcurrentHashMap<>();

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
        List<javafx.scene.control.MenuItem> l = new ArrayList<>(menu.getItems());
        //noinspection unchecked
        Collections.sort(l, (Comparator<javafx.scene.control.MenuItem>) (Object) Prioritised.COMPARATOR);
        menu.getItems().removeAll();
        l.forEach(mi -> menu.getItems().add(mi));
      }
    });
  }

  /** Return title for given category */
  public Integer actionCategoryPriority(String categoryId) {
    String val = MessageFactory.getInstance().getMessage(categoryId + "|priority", null);
    if (val == null) { return null; }
    return Integer.valueOf(val);
  }

  private void addMenuItem(Object item) {
    if (item instanceof EventHandler) { addEventHandlerToMenu((Action) item); }
    if (item instanceof MenuOptions) { addOptionsToMenu((MenuOptions) item); }
  }

  private Menu getMenuOfCategory(String[] cats) {
    String[] s = new String[cats.length - 1];
    System.arraycopy(cats, 1, s, 0, cats.length - 1);
    return getMenuOfCategory(s, 0);
  }

  private Menu getMenuOfCategory(String[] cats, int level) {
    StringBuilder catId = new StringBuilder();
    String parentId = "";
    for (int i = 0; i <= level; i++) {
      parentId = catId.toString();
      if (i > 0) { catId.append("/"); }
      catId.append(cats[i]);
    }

    MenuItem mi = this.category.get(catId.toString());
    if (mi instanceof PrioritisedMenu) {
      PrioritisedMenu m = (PrioritisedMenu) mi;
      if (level + 2 < cats.length) { return getMenuOfCategory(cats, level + 1); }
      return m;
    } else if (mi == null) {
      PrioritisedMenu m = new PrioritisedMenu(MessageFactory.getInstance().getMessage(MENU_CATEGORY + catId.toString()));
      m.setPriority(actionCategoryPriority(MENU_CATEGORY + catId.toString()));
      this.category.put(catId.toString(), m);
      if (level == 0) {
        menu.getItems().add(m);
      } else {
        PrioritisedMenu parent = (PrioritisedMenu) this.category.get(parentId);
        parent.getItems().add(m);
      }
      if (level + 2 < cats.length) { return getMenuOfCategory(cats, level + 1); }
      return m;
    }
    return null;
  }

  private Integer appendGroupSeparator(Menu menu, ActionConfig ac) {
    if (ac.category().lastIndexOf("/") == 0) { return 0; }
    String groupId = ac.category().substring(1, ac.category().length());
    PrioritisedSeparatorMenuItem menuItem = (PrioritisedSeparatorMenuItem) category.get(groupId);
    if (menuItem == null) {
      Integer priority = actionCategoryPriority(MENU_CATEGORY + groupId) * 10000000;
      if (priority == 0) { return 0; } // Skip first separator
      menuItem = new PrioritisedSeparatorMenuItem();
      menuItem.setPriority(priority);
      menu.getItems().add(menuItem);
      category.put(groupId, menuItem);
    }
    return menuItem.getPriority();
  }

  private void addEventHandlerToMenu(Action event) {
    ActionConfig ac = event.getClass().getAnnotation(ActionConfig.class);
    if (!ac.showInMenuButton()) { return; }
    Menu m = getMenuOfCategory(ac.category().split("/"));
    Integer catP = appendGroupSeparator(m, ac);
    ActionGUIConfig gui = ac.gui()[0];
    PrioritisedMenuItem item = new PrioritisedMenuItem(MessageFactory.getInstance().getMessage(gui.displayName()));
    item.setPriority(catP + ac.priority());
    m.getItems().add(item);
    item.setOnAction(event);
    //noinspection unchecked
    m.getItems().sort((Comparator<javafx.scene.control.MenuItem>) (Object) Prioritised.COMPARATOR);
  }

  private <F> void addOptionsToMenu(MenuOptions<F> options) {
    ActionConfig ac = options.getClass().getAnnotation(ActionConfig.class);
    if (!ac.showInMenuButton()) { return; }
    Menu m = getMenuOfCategory(ac.category().split("/"));
    Integer catP = appendGroupSeparator(m, ac);
    ActionGUIConfig gui = ac.gui()[0];
    PrioritisedMenu item = new PrioritisedMenu(MessageFactory.getInstance().getMessage(gui.displayName()));
    item.setPriority(catP + ac.priority());
    m.getItems().add(item);

    reloadMenuOptions(item, options);
    options.getItems().addListener((ListChangeListener<F>) change -> {
      while (change.next()) { reloadMenuOptions(item, options); }
    });
  }

  private <F> void reloadMenuOptions(Menu m, MenuOptions<F> options) {
    m.getItems().removeAll();
    options.getItems().forEach(it -> {
      CheckMenuItem mi = new CheckMenuItem(String.valueOf(it));
      mi.setSelected(options.isChecked(it));
      m.getItems().add(mi);
      mi.setOnAction(event -> {
        options.setSelect(it);
        options.handle(event);
      });
      options.checkedProperty(it).addListener((observable, oldValue, newValue) -> mi.setSelected(newValue));
    });
  }

  @Override
  protected Skin<?> createDefaultSkin() {
    return new RibbonMainButtonSkin(this);
  }

  public ContextMenu getMenu() {
    return menu;
  }
}
