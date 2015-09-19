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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 19.9.15.
 * Reimplementation of CheckBoxListCell because I want to catch on click event and consume it
 */
public class CheckBoxListCell<T> extends ListCell<T> {
  private final CheckBox checkBox;
  private ObservableValue<Boolean> booleanProperty;
  private ObjectProperty<StringConverter<T>> converter;
  private ObjectProperty<Callback<T, ObservableValue<Boolean>>> selectedStateCallback;

  private final StringConverter<T> defaultStringConverter = new StringConverter() {
    public String toString(Object var1) {
      return var1 == null?null:var1.toString();
    }

    public Object fromString(String var1) {
      return var1;
    }
  };

  public static <T> Callback<ListView<T>, ListCell<T>> forListView(Callback<T, ObservableValue<Boolean>> var0) {
    return forListView(var0, null);
  }

  public static <T> Callback<ListView<T>, ListCell<T>> forListView(Callback<T, ObservableValue<Boolean>> var0, StringConverter<T> var1) {
    return (var2) -> {
      return new CheckBoxListCell(var0, var1);
    };
  }

  public CheckBoxListCell() {
    this((Callback)null);
  }

  public CheckBoxListCell(Callback<T, ObservableValue<Boolean>> var1) {
    this(var1, null);
  }

  public CheckBoxListCell(Callback<T, ObservableValue<Boolean>> var1, StringConverter<T> var2) {
    if (var2 == null) {
      var2 = defaultStringConverter;
    }
    this.converter = new SimpleObjectProperty(this, "converter");
    this.selectedStateCallback = new SimpleObjectProperty(this, "selectedStateCallback");
    this.getStyleClass().add("check-box-list-cell");
    this.setSelectedStateCallback(var1);
    this.setConverter(var2);
    this.checkBox = new CheckBox();
    this.setAlignment(Pos.CENTER_LEFT);
    this.setContentDisplay(ContentDisplay.LEFT);
    this.setGraphic((Node) null);
    this.checkBox.setOnAction(event -> event.consume());
  }

  public final ObjectProperty<StringConverter<T>> converterProperty() {
    return this.converter;
  }

  public final void setConverter(StringConverter<T> var1) {
    this.converterProperty().set(var1);
  }

  public final StringConverter<T> getConverter() {
    return (StringConverter)this.converterProperty().get();
  }

  public final ObjectProperty<Callback<T, ObservableValue<Boolean>>> selectedStateCallbackProperty() {
    return this.selectedStateCallback;
  }

  public final void setSelectedStateCallback(Callback<T, ObservableValue<Boolean>> var1) {
    this.selectedStateCallbackProperty().set(var1);
  }

  public final Callback<T, ObservableValue<Boolean>> getSelectedStateCallback() {
    return (Callback)this.selectedStateCallbackProperty().get();
  }

  public void updateItem(T var1, boolean var2) {
    super.updateItem(var1, var2);
    if(!var2) {
      StringConverter var3 = this.getConverter();
      Callback var4 = this.getSelectedStateCallback();
      if(var4 == null) {
        throw new NullPointerException("The CheckBoxListCell selectedStateCallbackProperty can not be null");
      }

      this.setGraphic(this.checkBox);
      this.setText(var3 != null?var3.toString(var1):(var1 == null?"":var1.toString()));
      if(this.booleanProperty != null) {
        this.checkBox.selectedProperty().unbindBidirectional((BooleanProperty)this.booleanProperty);
      }

      this.booleanProperty = (ObservableValue)var4.call(var1);
      if(this.booleanProperty != null) {
        this.checkBox.selectedProperty().bindBidirectional((BooleanProperty)this.booleanProperty);
      }
    } else {
      this.setGraphic((Node)null);
      this.setText((String)null);
    }

  }
}

