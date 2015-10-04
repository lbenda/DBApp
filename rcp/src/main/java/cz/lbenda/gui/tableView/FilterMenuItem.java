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

import cz.lbenda.rcp.IconFactory;
import cz.lbenda.rcp.localization.Message;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Predicate;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 18.9.15.
 * Menu item which show list box */
public class FilterMenuItem extends MenuItem {

  public static final String ICON_SORT_ASC = "view-sort-ascending.png";
  public static final String ICON_SORT_DSC = "view-sort-descending.png";
  public static final String ICON_FILTER = "filter.png";

  @Message
  public static final String msgSortAsc = "Ascending";
  @Message
  public static final String msgSortDsc = "Descending";
  @Message
  public static final String msgFilterAll = "All";
  @Message
  public static final String msgOK = "OK";
  @Message
  public static final String msgCancel = "Cancel";

  private final HBox buttonBar = new HBox();
  private final BorderPane quickFilter = new BorderPane();
  private final HBox okCancelBar = new HBox();
  /** Set of names of items which already exist in items list */
  private final Set<String> existingItemNames = new ConcurrentSkipListSet<>();
  /** Set of names of chosen items */
  private final Set<String> chosenItemNames = new ConcurrentSkipListSet<>();
  /** List of all items which can be choose */
  private final ObservableMap<Object, Item> itemsMap = FXCollections.observableMap(new WeakHashMap<>());
  private final ObservableList<Item> items = FXCollections.observableArrayList();
  private final FilteredList<Item> filteredList = new FilteredList<>(items);

  private final ImageView sortIndicator = new ImageView();
  private final ImageView filterIndicator = IconFactory.getInstance().imageView(FilterMenuItem.class, ICON_FILTER, IconFactory.IconLocation.INDICATOR);

  /** Filters which was set to table view */
  private Predicate filter;

  /** Table which use this filter item */
  private final FilterableTableView filterableTableView;
  /** Table column on which is this filter menu item showed */
  private final FilterableTableColumn tableColumn;

  /** Sort togle group */
  private ToggleGroup sortToggleGroup = new ToggleGroup();
  /** Comparator which could be set to sorter */
  private Comparator comparatorAsc;
  /** Comparator which could be set to sorter */
  private Comparator comparatorDsc;
  /** Check box which check or un-check all items in list view */
  private final CheckBox cbAll = new CheckBox(msgFilterAll);

  public FilterMenuItem(FilterableTableView filterableTableView, FilterableTableColumn filterableTableColumn) {
    this(new VBox(), filterableTableView, filterableTableColumn);
  }

  private FilterMenuItem(VBox panel, FilterableTableView filterableTableView, FilterableTableColumn filterableTableColumn) {
    super(null, panel);
    this.filterableTableView = filterableTableView;
    okCancelBar.setAlignment(Pos.BOTTOM_RIGHT);
    this.tableColumn = filterableTableColumn;
    this.getStyleClass().add("listview-menu-item");

    SortedList<Item> sortedList = new SortedList<>(filteredList, Item::compareTo);
    ListView<Item> listView = new ListView<>();
    listView.setItems(sortedList);
    listView.setCellFactory(CheckBoxListCell.forListView(Item::onProperty));

    TextField textField = new TextField();
    textField.textProperty().addListener((observable, oldValue, newValue) -> {
      filteredList.setPredicate(item ->
        StringUtils.isEmpty(newValue) || !StringUtils.isEmpty(item.getName())
            && item.getName().toLowerCase().contains(newValue.toLowerCase()));
    });

    prepareBars();
    prepareQuickFilter();
    prepareOkCancelBar();
    panel.getChildren().add(buttonBar);
    panel.getChildren().add(textField);
    panel.getChildren().add(listView);
    panel.getChildren().add(quickFilter);
    panel.getChildren().add(okCancelBar);

    //noinspection unchecked
    filterableTableView.sortProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null || (newValue != comparatorAsc && newValue != comparatorDsc)) {
        filterableTableColumn.removeRightIndicator(sortIndicator);
        sortToggleGroup.selectToggle(null);
      } else {
        filterableTableColumn.removeRightIndicator(sortIndicator);
        filterableTableColumn.addRightIndicator(sortIndicator);
        if (newValue == comparatorAsc) {
          sortIndicator.setImage(IconFactory.getInstance().image(this, ICON_SORT_ASC,
              IconFactory.IconLocation.INDICATOR));
        } else {
          sortIndicator.setImage(IconFactory.getInstance().image(this, ICON_SORT_DSC,
              IconFactory.IconLocation.INDICATOR));
        }
      }
    });
  }

  /** Prepare buttons for bar with quick filter */
  private void prepareQuickFilter() {
    cbAll.setSelected(true);
    quickFilter.setLeft(cbAll);
    cbAll.setOnAction(Event::consume);
    cbAll.selectedProperty().addListener((observable, oldValue, newValue) -> {
      this.items.forEach(item -> item.setOn(newValue));
    });
  }

  /** Prepare buttons to button bar */
  private void prepareBars() {
    ToggleButton sortAsc = new ToggleButton(msgSortAsc,
        IconFactory.getInstance().imageView(this, ICON_SORT_ASC, IconFactory.IconLocation.LOCAL_TOOLBAR));
    ToggleButton sortDsc = new ToggleButton(msgSortDsc,
        IconFactory.getInstance().imageView(this, ICON_SORT_DSC, IconFactory.IconLocation.LOCAL_TOOLBAR));

    //noinspection unchecked
    final StringConverter converter = filterableTableView.stringConverter(tableColumn);

    comparatorAsc = (o1, o2) -> {
      //noinspection unchecked
      Object value1 = filterableTableView.valueForColumn(o1, tableColumn);
      //noinspection unchecked
      Object value2 = filterableTableView.valueForColumn(o2, tableColumn);
      if (value1 == null) {
        if (value2 == null) {
          return 0;
        } else {
          return 1;
        }
      } else if (value2 == null) {
        return -1;
      }
      if (value1 instanceof Comparable) {
        //noinspection unchecked
        return ((Comparable) value1).compareTo(value2);
      }
      //noinspection unchecked
      return converter.toString(value1).compareTo(converter.toString(value2));
    };
    comparatorDsc = comparatorAsc.reversed();

    sortToggleGroup.getToggles().addAll(sortAsc, sortDsc);

    sortAsc.setOnAction(event -> {
      Toggle tg = sortToggleGroup.getSelectedToggle();
      if (tg == sortDsc) {
        //noinspection unchecked
        filterableTableView.sortProperty().setValue(comparatorDsc);
      } else if (tg == sortAsc) {
        //noinspection unchecked
        filterableTableView.sortProperty().setValue(comparatorAsc);
      } else {
        //noinspection unchecked
        filterableTableView.sortProperty().setValue(null);
      }
    });
    sortDsc.setOnAction(sortAsc.getOnAction());
    buttonBar.getChildren().addAll(sortAsc, sortDsc);
  }

  /** Inform if this field hold filter on table */
  public boolean isFilter() {
    return !this.items.stream().allMatch(Item::isOn);
  }

  private void prepareOkCancelBar() {
    Button okButton = new Button(msgOK);
    okButton.setDefaultButton(true);
    Button cancelButton = new Button(msgCancel);
    cancelButton.setCancelButton(true);
    okCancelBar.getChildren().addAll(okButton, cancelButton);

    //noinspection unchecked
    final StringConverter converter = filterableTableView.stringConverter(tableColumn);
    okButton.setOnAction(event -> {
      if (filter != null) { filterableTableView.filters().remove(filter); }
      if (!isFilter()) { tableColumn.removeLeftIndicator(filterIndicator);
      } else {
        tableColumn.addLeftIndicator(filterIndicator);
        //noinspection unchecked
        filter = row -> {
          //noinspection unchecked
          Object value = filterableTableView.valueForColumn(row, tableColumn);
          String text;
          if (value == null) { text = ""; }
          else { //noinspection unchecked
            text = converter.toString(value);
          }
          return chosenItemNames.contains(text);
        };
        //noinspection unchecked
        filterableTableView.filters().add(filter);
      }
    });
  }

  /** Init menu item before is showed to user */
  @SuppressWarnings("unchecked")
  public void beforeOpenInit() {
    StringConverter converter = filterableTableView.stringConverter(tableColumn);
    items.clear();
    existingItemNames.clear();
    filterableTableView.valuesForColumn(tableColumn).filter(value -> {
      if (value == null) { return existingItemNames.add(""); }
      return existingItemNames.add(converter.toString(value));
    }).forEach(value -> {
      Item item = itemsMap.get(value);
      if (item != null) { items.add(item); }
      else {
        if (value == null) { item = new Item(null, true); }
        else { item = new Item(converter.toString(value), true); }
        items.add(item);
        itemsMap.put(value, item);
      }
    });
  }

  public class Item implements Comparable<Item> {
    private final StringProperty nameProperty = new SimpleStringProperty();
    private final BooleanProperty onProperty = new SimpleBooleanProperty();

    public Item(String name, boolean on) {
      onProperty.addListener((observable, oldValue, newValue) -> {
        if (newValue) { chosenItemNames.add(getName()); }
        else { chosenItemNames.remove(getName()); }
      });
      setName(name);
      setOn(on);
    }

    public final StringProperty nameProperty() { return this.nameProperty; }
    public final String getName() { return this.nameProperty().get(); }
    public final void setName(String name) {
      if (name == null) { name = ""; }
      this.nameProperty().set(name);
    }

    public final BooleanProperty onProperty() { return this.onProperty; }
    public final boolean isOn() { return this.onProperty().get(); }
    public final void setOn(final boolean on) { this.onProperty().set(on); }

    @Override
    public String toString() {
      return getName();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Item)) return false;

      Item item = (Item) o;

      if (item.getName() == null) { return getName() == null; }
      else if (getName() == null) { return false;  }
      return getName().equals(item.getName());
    }

    @Override
    public int hashCode() {
      if (getName() == null) { return 0;
      }
      return getName().hashCode();
    }

    @Override
    public int compareTo(@Nonnull Item o) {
      if (!this.getClass().equals(o.getClass())) { throw new ClassCastException("The compared object must be same. Expected: "
          + getClass().getName() + " but compared object is: " + o.getClass().getName()); }
      if (o.getName() == null) {
        if (getName() == null) { return 0; }
        else { return -1; }
      } else if (getName() == null) { return 1; }
      return getName().compareTo(o.getName());
    }
  }
}
