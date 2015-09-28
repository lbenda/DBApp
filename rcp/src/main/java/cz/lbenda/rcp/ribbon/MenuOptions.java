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
package cz.lbenda.rcp.ribbon;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 12.9.15.
 * Interface which is used as options which can user choose from menu */
public interface MenuOptions<T> {
  /** Return all items which options holds */
  ObservableList<T> getItems();
  /** Return text option for item  */
  String itemToString(T item);
  /** Return item for text  */
  T stringToItem(String name);
  /** Method which is call when item is selected
   * @param item item which was selected */
  void setSelect(T item);
  /** Return current selected item */
  T getSelect();
  /** Property which hold current selected item */
  ObjectProperty<T> selectProperty();

  /** Execute default handle operation */
  void handle(ActionEvent stage);

  /** Inform if item will be checked or not */
  boolean isChecked(T item);
  ReadOnlyBooleanProperty checkedProperty(T item);
}
