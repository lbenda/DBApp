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

import cz.lbenda.common.StringConverters;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.util.StringConverter;

import java.util.stream.Stream;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 15.10.15.
 * Simple implementation which user string values for column */
@SuppressWarnings("unused")
public class SimpleTableView<S> extends FilterableTableView<S> {
  @Override
  public <T> Stream<T> valuesForColumn(TableColumn<S, ?> tableColumn) {
    return getItems().stream().map(row -> {
      ObservableValue<?> observableValue = tableColumn.getCellObservableValue(row);
      //noinspection unchecked
      return (T) observableValue.getValue();
    });
  }

  @Override
  public <T> StringConverter<T> stringConverter(TableColumn<S, ?> tableColumn) {
    //noinspection unchecked
    return (StringConverter<T>) StringConverters.OBJECT_CONVERTER;
  }

  @Override
  public <T> T valueForColumn(S row, TableColumn<S, ?> tableColumn) {
    ObservableValue<?> observableValue = tableColumn.getCellObservableValue(row);
    //noinspection unchecked
    return (T) observableValue.getValue();
  }
}
