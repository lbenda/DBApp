package cz.lbenda.rcp.ribbon;

import cz.lbenda.rcp.localization.MessageFactory;
import cz.lbenda.rcp.ribbon.skin.RibbonSkin;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;

import java.util.Collection;
import java.util.HashMap;

/**
 * Created by pedro_000 on 1/18/14.
 */
public class Ribbon extends Control{
  public final static String DEFAULT_STYLE_CLASS = "ribbon";

  private ObservableList<String> tabTitles;
  private ObservableList<RibbonTab> tabs;

  private HashMap<String, RibbonTab> titleToRibbonTab;

  private RibbonQuickAccessBar quickAccessBar;

  private RibbonItemFactory itemFactory; public RibbonItemFactory getItemFactory() { return itemFactory; }
  private MessageFactory messageFactory; public MessageFactory getMessageFactory() { return messageFactory; }
  public void setMessageFactory(MessageFactory messageFactory) {
    this.messageFactory = messageFactory;
    itemFactory.setMessageFactory(messageFactory);
  }

  public Ribbon() {
    this(MessageFactory.getInstance());
  }

  public Ribbon(MessageFactory messageFactory) {
    itemFactory = new RibbonItemFactory(this, messageFactory);
    quickAccessBar = new RibbonQuickAccessBar();

    tabTitles = FXCollections.observableArrayList();
    tabs = FXCollections.observableArrayList();
    titleToRibbonTab = new HashMap<>();

    tabTitles.addListener(new ListChangeListener<String>() {
      @Override
      public void onChanged(Change<? extends String> changed) {
        tabTitlesChanged(changed);
      }
    });

    getStyleClass().setAll(DEFAULT_STYLE_CLASS);
  }

  private void tabTitlesChanged(ListChangeListener.Change<? extends String> changed) {
    while(changed.next()) {
      if (changed.wasAdded()) {
        updateAddedRibbonTabs(changed.getAddedSubList());
      }
      if(changed.wasRemoved()) {
        for (String title : changed.getRemoved())
          titleToRibbonTab.remove(title);
      }
    }
  }

  public RibbonTab tabByTitle(String title) {
    return titleToRibbonTab.get(title);
  }

  private void updateAddedRibbonTabs(Collection<? extends String> ribbonTabTitles) {
    for (String title : ribbonTabTitles) {
      RibbonTab ribbonTab = new RibbonTab(title);
      titleToRibbonTab.put(title, ribbonTab);
      tabs.add(ribbonTab);
    }
  }

  public ObservableList<String> getTabTitles() {
    return tabTitles;
  }

  public ObservableList<RibbonTab> getTabs() {
    return tabs;
  }

  public RibbonQuickAccessBar getQuickAccessBar() {
    return quickAccessBar;
  }
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
