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
package cz.lbenda.gui.tableView;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.apache.commons.lang3.StringUtils;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 19.9.15.
 * Column for filterable table */
public class FilterableTableColumn<S, T> extends TableColumn<S, T> {

  /** Indicator pane on left side of title */
  private HBox leftIndicatorPane = new HBox();
  /** Indicator pane on right side of title */
  private HBox rightIndicatorPane = new HBox();
  /** Title of column */
  private Label title = new Label();

  public FilterableTableColumn() {
    super();
    BorderPane bPane = new BorderPane();
    leftIndicatorPane.setAlignment(Pos.CENTER_LEFT);
    rightIndicatorPane.setAlignment(Pos.CENTER_RIGHT);
    bPane.setLeft(leftIndicatorPane);
    bPane.setRight(rightIndicatorPane);
    bPane.setCenter(title);
    this.setGraphic(bPane);
  }

  @SuppressWarnings("unused")
  public FilterableTableColumn(String label, String tooltip) {
    this();
    setTitle(label);
    if (!StringUtils.isEmpty(tooltip)) { title.setTooltip(new Tooltip(tooltip)); }
  }

  public void setTitle(String title) {
    this.title.setText(title);
  }

  public String getTitle() {
    return this.title.getText();
  }

  /** Set indicator to left pane indicator */
  @SuppressWarnings("unused")
  public void addLeftIndicator(Node indicator) {
    if (!leftIndicatorPane.getChildren().contains(indicator)) {
      leftIndicatorPane.getChildren().add(indicator);
    }
  }
  /** Remove indicator from left indicator pane */
  @SuppressWarnings("unused")
  public void removeLeftIndicator(Node indicator) { leftIndicatorPane.getChildren().remove(indicator); }

  /** Set indicator to right pane indicator */
  @SuppressWarnings("unused")
  public void addRightIndicator(Node indicator) {
    if (!rightIndicatorPane.getChildren().contains(indicator)) {
      rightIndicatorPane.getChildren().add(indicator);
    }
  }
  /** Remove indicator from right indicator pane */
  @SuppressWarnings("unused")
  public void removeRightIndicator(Node indicator) { rightIndicatorPane.getChildren().remove(indicator); }
}
