package cz.lbenda.rcp.ribbon;

import cz.lbenda.rcp.localization.MessageFactory;
import cz.lbenda.rcp.ribbon.skin.RibbonSkin;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;

import javax.annotation.Nonnull;
import java.util.Collections;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Implementation of ribbon menu. */
public class Ribbon extends Control {
  public final static String DEFAULT_STYLE_CLASS = "ribbon";

  private final ObservableList<RibbonTab> tabs = FXCollections.observableArrayList();

  private RibbonQuickAccessBar quickAccessBar;

  private RibbonItemFactory itemFactory; public RibbonItemFactory getItemFactory() { return itemFactory; }

  @SuppressWarnings("unused")
  public Ribbon() {
    this(MessageFactory.getInstance());
  }

  public Ribbon(MessageFactory messageFactory) {
    tabs.addListener((ListChangeListener<RibbonTab>) change -> {
      while (change.next()) {
        if (change.wasAdded()) { Collections.sort(tabs, Prioritised.COMPARATOR); }
      }
    });
    itemFactory = new RibbonItemFactory(this, messageFactory);
    quickAccessBar = new RibbonQuickAccessBar();
    getStyleClass().setAll(DEFAULT_STYLE_CLASS);
  }

  public RibbonTab tabById(@Nonnull String id) {
    for (RibbonTab tab : this.tabsProperty()) { if (id.equals(tab.getId())) { return tab; }  }
    return null;
  }

  public ObservableList<RibbonTab> tabsProperty() {
    return tabs;
  }

  public RibbonQuickAccessBar getQuickAccessBar() {
    return quickAccessBar;
  }
  @SuppressWarnings("unused")
  public void setQuickAccessBar(RibbonQuickAccessBar qAccessBar) {
    quickAccessBar = qAccessBar;
  }

  @Override
  protected Skin<?> createDefaultSkin() {
    return new RibbonSkin(this);
  }

  /***************************************************************************
   *                                                                         *
   * Properties                                                              *
   *                                                                         *
   **************************************************************************/

  /** Selected Ribbon Tab **/

  private SimpleObjectProperty<RibbonTab> selectedRibbonTab = new SimpleObjectProperty<>();

  public SimpleObjectProperty selectedRibbonTabProperty() {
    return selectedRibbonTab;
  }
  @SuppressWarnings("unused")
  public RibbonTab getSelectedRibbonTab() {
    return selectedRibbonTab.get();
  }
  public void setSelectedRibbonTab(RibbonTab ribbonTab) {
    selectedRibbonTab.set(ribbonTab);
  }

  @Override
  public String getUserAgentStylesheet() {
    return this.getClass().getResource("fxribbon.css").toExternalForm();
  }
}
