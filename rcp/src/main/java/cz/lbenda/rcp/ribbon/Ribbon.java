package cz.lbenda.rcp.ribbon;

import cz.lbenda.rcp.ribbon.skin.RibbonSkin;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.annotation.Nonnull;
import java.util.Collections;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Implementation of ribbon menu. */
public class Ribbon extends Control {
  public final static String DEFAULT_STYLE_CLASS = "ribbon";

  private final ObservableList<RibbonTab> tabs = FXCollections.observableArrayList();

  private RibbonQuickAccessBar quickAccessBar;
  private RibbonMainButton mainButton; public RibbonMainButton getMainButton() { return mainButton; }
  private RibbonItemFactory itemFactory; public RibbonItemFactory getItemFactory() { return itemFactory; }

  /** Create ribbon menu including change window decoration. The window is primary stage
   * @param primaryStage primary stage of application
   * @param appName name of app which is add to main button
   * @param appImg image of app which is used as icon on main button */
  public Ribbon(Stage primaryStage, String appName, Image appImg) {
    // primaryStage.initStyle(StageStyle.UNDECORATED);
    tabs.addListener((ListChangeListener<RibbonTab>) change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          Collections.sort(tabs, Prioritised.COMPARATOR);
        }
      }
    });
    itemFactory = new RibbonItemFactory(this);
    quickAccessBar = new RibbonQuickAccessBar();
    mainButton = new RibbonMainButton(appName, appImg);
    getStyleClass().setAll(DEFAULT_STYLE_CLASS);
  }

  public RibbonTab tabById(@Nonnull String id) {
    for (RibbonTab tab : this.tabsProperty()) { if (id.equals(tab.getId())) { return tab; }  }
    return null;
  }

  public ObservableList<RibbonTab> tabsProperty() {
    return tabs;
  }

  public RibbonQuickAccessBar getQuickAccessBar() { return quickAccessBar; }

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

  class WindowButtons extends HBox {
    public WindowButtons() {
      Button closeBtn = new Button("X");
      closeBtn.setOnAction(actionEvent -> Platform.exit());

      this.getChildren().add(closeBtn);
    }
  }
}
