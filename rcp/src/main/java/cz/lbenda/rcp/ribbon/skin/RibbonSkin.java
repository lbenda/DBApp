package cz.lbenda.rcp.ribbon.skin;

import cz.lbenda.rcp.ribbon.Ribbon;
import cz.lbenda.rcp.ribbon.RibbonTab;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.SkinBase;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;

import java.util.Collection;

/** Created by pedro_000 on 1/18/14.
 * Skin for whole ribbon */
public class RibbonSkin extends SkinBase<Ribbon> {
  private TabPane tabPane;

  /** Constructor for all SkinBase instances.
   * @param control The control for which this Skin should attach to. */
  public RibbonSkin(Ribbon control) {
    super(control);
    tabPane = new TabPane();
    VBox outerContainer = new VBox();

    control.getTabs().addListener(this::tabsChanged);
    updateAddedRibbonTabs(control.getTabs());
    outerContainer.getStyleClass().setAll("outer-container");
    outerContainer.getChildren().addAll(control.getQuickAccessBar(), tabPane);
    getChildren().add(outerContainer);
    control.selectedRibbonTabProperty().addListener((ChangeListener) (observable, oldValue, newValue) -> tabPane.getSelectionModel().select((RibbonTab)newValue));
    tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      control.setSelectedRibbonTab((RibbonTab)tabPane.getSelectionModel().getSelectedItem());
    });
  }

  private void updateAddedRibbonTabs(Collection<? extends RibbonTab> ribbonTabs) {
    ribbonTabs.forEach(ribbonTab -> tabPane.getTabs().add(ribbonTab));
  }

  private void tabsChanged(ListChangeListener.Change<? extends RibbonTab> changed) {
    while (changed.next()) {
      if (changed.wasAdded()) { updateAddedRibbonTabs(changed.getAddedSubList()); }
      else if (changed.wasRemoved()) {
        for (RibbonTab ribbonTab : changed.getRemoved()) { tabPane.getTabs().remove(ribbonTab); }
      }
    }
  }
}
