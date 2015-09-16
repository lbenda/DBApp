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
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Factory which create action and holder from given action configuration */
public class RibbonItemFactory<T> {

  private static final Logger LOG = LoggerFactory.getLogger(RibbonItemFactory.class);

  private Map<ActionConfig, Node> nodeForAction = new HashMap<>();

  private enum IconSize {
    SMALL(16), MEDIUM(24), LARGE(32), ;
    int size;
    IconSize(int size) { this.size = size; }
    public int size() { return size; }
  }

  private ObservableList<T> itemsHandler = FXCollections.observableArrayList(); public ObservableList<T> getItemsHandler() { return itemsHandler; }
  private Ribbon ribbon;
  private MessageFactory messageFactory; public void setMessageFactory(MessageFactory messageFactory) { this.messageFactory = messageFactory; }
  private IconSize iconSize = IconSize.LARGE;

  public RibbonItemFactory(Ribbon ribbon, MessageFactory messageFactory) {
    this.messageFactory = messageFactory;
    this.ribbon = ribbon;
    itemsHandler.addListener((ListChangeListener<T>) change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          change.getAddedSubList().forEach(this::addItemToMenu);
        }
      }
    });
  }

  public void addItemToMenu(T item) {
    if (item instanceof EventHandler) { addEventHandlerToMenu((Action) item); }
    if (item instanceof MenuOptions) { addOptionsToMenu((MenuOptions) item); }
  }

  private RibbonGroup prepareGroup(ActionConfig ac) {
    if (ac == null || ac.category() == null || ac.id() == null) {
      throw new IllegalArgumentException("The event handler haven't defined ActionConfiguration or haven't specified all method of this annotation.");
    }

    String[] menuParts = ac.category().split("/");
    if (menuParts.length < 3) { throw new IllegalArgumentException("The category have less then 3 parts."); }

    String groupId = "ribbonGroup_/" + menuParts[1] + "/" + menuParts[2];

    RibbonTab tab = ribbon.tabByTitle(messageFactory.actionCategoryTitle("/" + menuParts[1]));
    RibbonGroup group = null;

    if (tab != null) {
      for (RibbonGroup rg : tab.getRibbonGroups()) { if (groupId.equals(rg.getId())) { group = rg; } }
    } else {
      ribbon.getTabTitles().add(messageFactory.actionCategoryTitle("/" + menuParts[1]));
      tab = ribbon.tabByTitle(messageFactory.actionCategoryTitle("/" + menuParts[1]));
      // this.ribbon.getTabs().add(tab);
    }
    if (group == null) {
      group = new RibbonGroup();
      group.setId(groupId);
      group.setTitle(messageFactory.actionCategoryTitle("/" + menuParts[1] + "/" + menuParts[2]));
      tab.getRibbonGroups().add(group);
    }
    return group;
  }

  private <F> void addOptionsToMenu(MenuOptions<F> options) {
    ActionConfig ac = options.getClass().getAnnotation(ActionConfig.class);
    String itemId = "ribbonOptions_" + ac.id();
    RibbonGroup group = prepareGroup(ac);

    RibbonItem ri = new RibbonItem();
    if (!"".equals(ac.gui()[0].iconBase())) {
      ri.setGraphic(new ImageView(generateIcon(options.getClass(), ac.gui()[0].iconBase(), iconSize)));
    }
    ComboBox<F> cb = new ComboBox<>();
    cb.setId(itemId);
    cb.setItems(options.getItems());
    cb.setPromptText(messageFactory.getMessage(ac.gui()[0].displayName()));
    Tooltip tp = new Tooltip(messageFactory.getMessage(ac.gui()[0].displayTooltip()));
    cb.setTooltip(tp);

    cb.valueProperty().addListener((observableValue, f, t1) -> {
      options.onSelect(t1);
    });
    ri.setItem(cb);

    putNodeToGroup(group, ri, ac);
  }

  private void addEventHandlerToMenu(Action event) {
    ActionConfig ac = event.getClass().getAnnotation(ActionConfig.class);
    String itemId = "ribbonButton_" + ac.id();
    RibbonGroup group = prepareGroup(ac);

    Button button = new Button();
    button.setId(itemId);
    button.setContentDisplay(ContentDisplay.TOP);
    button.getStyleClass().add("big");
    button.setWrapText(true);
    button.setDisable(!event.isEnable());
    button.setOnAction(event);

    actionGUIConfigToButton(ac.gui()[event.getConfig()], button, event);
    event.addChangeActionConfigConsumer(i -> {
      actionGUIConfigToButton(ac.gui()[i], button, event);
    });
    event.addEnableDisableConsumer(enabled -> button.setDisable(!enabled));

    putNodeToGroup(group, button, ac);
  }

  private String iconName(String base, IconSize iconSize) {
    switch (iconSize) {
      case SMALL : return base;
      case MEDIUM:
      case LARGE:
        String ext = FilenameUtils.getExtension(base);
        return FilenameUtils.removeExtension(base) + iconSize.size() + (StringUtils.isBlank(ext) ? "" : "." + ext);
    }
    return base;
  }

  private Image generateIcon(Class clazz, String base, IconSize iconSize) {
    String iconName = iconName(base, iconSize);
    InputStream is = clazz.getResourceAsStream(iconName);
    if (is == null) {
      String iconName2 = iconName("unknown.png", iconSize);
      LOG.warn("Icon with name not exist '" + iconName + "' '" + iconName2 + "' used instead of.");
      is = getClass().getResourceAsStream(iconName2);
    }
    return new Image(is);
  }

  private void actionGUIConfigToButton(ActionGUIConfig agc, Button button, Action event) {
    button.setText(messageFactory.getMessage(agc.displayName()));
    Tooltip tp = new Tooltip(messageFactory.getMessage(agc.displayTooltip()));
    button.setTooltip(tp);
    if (agc.iconBase() != null) {
      button.setGraphic(new ImageView(generateIcon(event.getClass(), agc.iconBase(), iconSize)));
    }
  }

  private void putNodeToGroup(RibbonGroup group, Node node, ActionConfig ac) {
    this.nodeForAction.put(ac, node);
    final List<ActionConfig> acsInGroup = new ArrayList<>();
    nodeForAction.keySet().stream()
        .filter(actionConfig -> ac.category().equals(actionConfig.category()))
        .sorted((o1, o2) -> Integer.compare(o1.priority(), o2.priority())).forEachOrdered(acsInGroup::add);
    int index = acsInGroup.indexOf(ac);
    group.getNodes().add(index, node);
  }
}
