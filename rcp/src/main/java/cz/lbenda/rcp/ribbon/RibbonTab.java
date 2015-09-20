package cz.lbenda.rcp.ribbon;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;

import java.util.Collections;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Implementation of tab in ribbon menu. */
public class RibbonTab extends Tab implements Prioritised {
  public static final String DEFAULT_STYLE_CLASS = "ribbon-tab";

  private HBox content;
  private ObservableList<RibbonGroup> ribbonGroups;
  /** Priority of tab in menu */
  private Integer priority; public Integer getPriority() { return priority; } public void setPriority(Integer priority) { this.priority = priority; }

  private String contextualColor;

  @SuppressWarnings("unused")
  public RibbonTab() {
    init();
  }

  public RibbonTab(String title) {
    super(title);
    init();
  }

  private void init() {
    ribbonGroups = FXCollections.observableArrayList();
    content = new HBox();
    this.setContent(content);

    setClosable(false);

    ribbonGroups.addListener(this::groupsChanged);
    content.getStyleClass().setAll(DEFAULT_STYLE_CLASS, "tab");
    getStyleClass().addListener((ListChangeListener<String>) c -> {
      while (c.next()) {
        if (c.wasAdded()) {
          for (String style : c.getAddedSubList()) {
            content.getStyleClass().add(style);
          }
        }
      }
    });
  }

  @SuppressWarnings("unused")
  public void setContextualColor(String color) {
    contextualColor = color;
    getStyleClass().add(color);
  }

  @SuppressWarnings("unused")
  public String getContextualColor() {
    return contextualColor;
  }

  private void groupsChanged(ListChangeListener.Change<? extends RibbonGroup> changed) {
    while (changed.next()) {
      if (changed.wasAdded()) { updateAddedGroups(); }
      if (changed.wasRemoved()) {
        for (RibbonGroup group : changed.getRemoved()) {
          int groupIndex = content.getChildren().indexOf(group);
          if (groupIndex != 0) {
            content.getChildren().remove(groupIndex - 1); // Remove separator }
            content.getChildren().remove(group);
          }
        }
      }
    }
  }

  private void updateAddedGroups() {
    Collections.sort(ribbonGroups, Prioritised.COMPARATOR);
    content.getChildren().clear();
    content.getChildren().addAll(ribbonGroups);
  }

  public ObservableList<RibbonGroup> getRibbonGroups() {
    return ribbonGroups;
  }
}
