package cz.lbenda.rcp.ribbon;

import cz.lbenda.rcp.ribbon.skin.RibbonQuickAccessBarSkin;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;

/** Created by pedro_000 on 2/19/14. */
public class RibbonQuickAccessBar extends Control {
  public final static String DEFAULT_STYLE_CLASS = "quick-access-bar";

  private ObservableList<Button> buttons;
  private ObservableList<Button> rightButtons;

  public RibbonQuickAccessBar() {
    buttons = FXCollections.observableArrayList();
    rightButtons = FXCollections.observableArrayList();

    getStyleClass().setAll(DEFAULT_STYLE_CLASS);
  }

  public ObservableList<Button> getButtons(){
    return buttons;
  }

  public ObservableList<Button> getRightButtons()
    {
        return rightButtons;
    }

  @Override
  protected Skin<?> createDefaultSkin() {
        return new RibbonQuickAccessBarSkin(this);
    }
}
