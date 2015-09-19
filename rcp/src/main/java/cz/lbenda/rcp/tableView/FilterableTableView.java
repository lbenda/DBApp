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
package cz.lbenda.rcp.tableView;

import cz.lbenda.rcp.localization.MessageFactory;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;

import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Table view with own implementation of sort and implementation of filter actions
 *  Created by Lukas Benda <lbenda @ lbenda.cz> on 18.9.15. */
public abstract class FilterableTableView<S> extends TableView<S> {

  private ObservableList<Predicate<S>> filters = FXCollections.observableArrayList();

  private ObservableList<S> items = FXCollections.observableArrayList();
  private FilteredList<S> filteredList = new FilteredList<>(items);
  private SortedList<S> sortedList = new SortedList<>(filteredList);
  private ObjectProperty<Comparator<? super S>> sortProperty = new SimpleObjectProperty<>();

  public FilterableTableView() {
    MessageFactory.initializeMessages(this);
    filters.addListener((ListChangeListener<Predicate<S>>) change ->
        filteredList.setPredicate(object -> filters.stream().allMatch(p -> p.test(object))));
    sortProperty.addListener((observable, oldValue, newValue) -> sortedList.setComparator(newValue));

    getColumns().addListener((ListChangeListener<TableColumn<S, ?>>) change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          //noinspection unchecked
          change.getAddedSubList().forEach(tableColumn -> putContextMenu((FilterableTableColumn) tableColumn));
        }
      }
    });
  }

  public final ObjectProperty<Comparator<? super S>> sortProperty() {
    return sortProperty;
  }

  /** Refresh viewed data - rexecute filter */
  public void refilter() {
    filteredList.setPredicate(object -> filters.stream().allMatch(p -> p.test(object)));
  }

  /** Append context menu into table column */
  private void putContextMenu(FilterableTableColumn<S, ?> tableColumn) {
    ContextMenu contextMenu = new ContextMenu();
    contextMenu.setAutoHide(false);
    FilterMenuItem filterMenuItem = new FilterMenuItem(this, tableColumn);
    contextMenu.getItems().add(filterMenuItem);
    contextMenu.setOnShowing(event -> filterMenuItem.beforeOpenInit());
    tableColumn.setContextMenu(contextMenu);
  }

  public void setRows(ObservableList<S> items) {
    this.items = items;
    filteredList = new FilteredList<>(items);
    sortedList = new SortedList<>(filteredList);
    super.setItems(sortedList);
  }

  public ObservableList<S> getRows() {
    return items;
  }

  /** Return stream with values from column which are in rows in given column */
  public abstract <T> Stream<T> valuesForColumn(TableColumn<S, ?> tableColumn);
  /** String converter for given column */
  public abstract <T> StringConverter<T> stringConverter(TableColumn<S, ?> tableColumn);
  /** Return value for given column */
  public abstract <T> T valueForColumn(S row, TableColumn<S, ?> tableColumn);

  /** List with all predicates which is used as filter */
  public ObservableList<Predicate<S>> filters() { return filters; }
}
