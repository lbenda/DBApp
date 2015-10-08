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

import cz.lbenda.rcp.IconFactory;
import cz.lbenda.rcp.action.Action;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.MessageFactory;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.util.*;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Factory which create action and holder from given action configuration */
public class RibbonItemFactory {

  private Map<ActionConfig, Node> nodeForAction = new HashMap<>();

  private Ribbon ribbon;
  private MessageFactory messageFactory = MessageFactory.getInstance();

  public RibbonItemFactory(Ribbon ribbon) {
    this.ribbon = ribbon;
    ribbon.itemsProperty().addListener((ListChangeListener<Object>) change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          change.getAddedSubList().forEach(this::addItemToMenu);
        }
        // TODO removed
      }
    });
  }

  public void addItemToMenu(Object item) {
    if (item instanceof MenuOptions) { addOptionsToMenu((MenuOptions) item); }
    else if (item instanceof EventHandler) { addEventHandlerToMenu((Action) item); }
  }

  /** Return title for given category */
  public Integer actionCategoryPriority(String categoryId) {
    String val = messageFactory.getMessage(categoryId + "|priority", null);
    if (val == null) { return null; }
    return Integer.valueOf(val);
  }

  private RibbonGroup prepareGroup(ActionConfig ac) {
    if (ac == null || ac.category() == null || ac.id() == null) {
      throw new IllegalArgumentException("The event handler haven't defined ActionConfiguration or haven't specified all method of this annotation.");
    }

    String[] menuParts = ac.category().split("/");
    if (menuParts.length < 3) { throw new IllegalArgumentException("The category have less then 3 parts.");
    }

    String tabId = "ribbonCategory_/" + menuParts[1];
    String groupId = tabId + "/" + menuParts[2];

    String tabTitle = messageFactory.getMessage(tabId);
    RibbonTab tab = ribbon.tabById(tabId);
    RibbonGroup group = null;

    if (tab != null) {
      for (RibbonGroup rg : tab.getRibbonGroups()) { if (groupId.equals(rg.getId())) { group = rg; } }
    } else {
      tab = new RibbonTab(tabTitle);
      tab.setId(tabId);
      tab.setPriority(actionCategoryPriority(tabId));
      ribbon.tabsProperty().add(tab);
    }
    if (group == null) {
      group = new RibbonGroup();
      group.setId(groupId);
      group.setTitle(messageFactory.getMessage(groupId));
      group.setPriority(actionCategoryPriority(groupId));
      tab.getRibbonGroups().add(group);
    }
    return group;
  }

  private <F> void addOptionsToMenu(MenuOptions<F> options) {
    ActionConfig ac = options.getClass().getAnnotation(ActionConfig.class);
    if (!ac.showInRibbon()) { return; }
    String itemId = "ribbonOptions_" + ac.id();
    RibbonGroup group = prepareGroup(ac);

    RibbonItem ri = new RibbonItem();
    if (!"".equals(ac.gui()[0].iconBase())) {
      ri.setGraphic(IconFactory.getInstance().imageView(options, ac.gui()[0].iconBase(), IconFactory.IconLocation.GLOBAL_TOOL_BAR));
    }

    if (options.isCheckBox()) {
      CheckBox cb = new CheckBox();
      cb.setId(itemId);
      cb.setText(messageFactory.getMessage(ac.gui()[0].displayName()));
      Tooltip tp = new Tooltip(messageFactory.getMessage(ac.gui()[0].displayTooltip()));
      cb.setTooltip(tp);
      cb.setSelected((Boolean) options.getSelect());
      //noinspection unchecked
      cb.selectedProperty().addListener((observable, oldValue, newValue) -> options.setSelect((F) newValue));
      options.selectProperty().addListener((observable, oldValue, newValue) -> cb.selectedProperty().setValue((Boolean) newValue));
      ri.setItem(cb);
    } else {
      ComboBox<F> cb = new ComboBox<>();
      cb.setId(itemId);
      cb.setItems(options.getItems());
      cb.setPromptText(messageFactory.getMessage(ac.gui()[0].displayName()));
      Tooltip tp = new Tooltip(messageFactory.getMessage(ac.gui()[0].displayTooltip()));
      cb.setTooltip(tp);
      cb.valueProperty().addListener((observableValue, f, t1) -> options.setSelect(t1));
      options.selectProperty().addListener((observable, oldValue, newValue) -> {
        cb.getSelectionModel().select(newValue);
      });
      ri.setItem(cb);
    }
    putNodeToGroup(group, ri, ac);
  }

  private void addEventHandlerToMenu(Action event) {
    ActionConfig ac = event.getClass().getAnnotation(ActionConfig.class);
    if (!ac.showInRibbon()) { return; }
    String itemId = "ribbonButton_" + ac.id();
    RibbonGroup group = prepareGroup(ac);

    Button button = new Button();
    button.setId(itemId);
    button.setContentDisplay(ContentDisplay.TOP);
    button.getStyleClass().add("big");
    button.setWrapText(true);
    button.setDisable(!event.isEnable());
    button.setOnAction(event);

    actionGUIConfigToButton(ac.gui()[0], button, event);
    event.addChangeActionConfigConsumer(i -> actionGUIConfigToButton(ac.gui()[i], button, event));
    event.addEnableDisableConsumer(enabled -> button.setDisable(!enabled));

    putNodeToGroup(group, button, ac);
  }

  private void actionGUIConfigToButton(ActionGUIConfig agc, Button button, Action event) {
    button.setText(messageFactory.getMessage(agc.displayName()));
    Tooltip tp = new Tooltip(messageFactory.getMessage(agc.displayTooltip()));
    button.setTooltip(tp);
    if (agc.iconBase() != null) {
      button.setGraphic(IconFactory.getInstance().imageView(event, agc.iconBase(), IconFactory.IconLocation.GLOBAL_TOOL_BAR));
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
