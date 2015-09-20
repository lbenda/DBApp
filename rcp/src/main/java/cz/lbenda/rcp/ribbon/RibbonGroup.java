package cz.lbenda.rcp.ribbon;

import cz.lbenda.rcp.ribbon.skin.RibbonGroupSkin;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.Skin;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Implementation of single group in ribbon menu. */
public class RibbonGroup extends Labeled implements Prioritised {

  public final static String DEFAULT_STYLE_CLASS = "ribbon-group";
  /** Priority of tab in menu */
  private Integer priority; public Integer getPriority() { return priority; } public void setPriority(Integer priority) { this.priority = priority; }

  private ObservableList<Node> nodes;
  private SimpleStringProperty title;

  public RibbonGroup() {
    nodes = FXCollections.observableArrayList();
    title = new SimpleStringProperty("");

    getStyleClass().setAll(DEFAULT_STYLE_CLASS);
  }

  public ObservableList<Node> getNodes() {
    return nodes;
  }

  public void setTitle(String title) {
    this.title.set(title);
  }

  public String getTitle() {
    return title.get();
  }

  public StringProperty titleProperty() {
    return title;
  }

  @Override
  protected Skin<?> createDefaultSkin() {
    return new RibbonGroupSkin(this);
  }
}
