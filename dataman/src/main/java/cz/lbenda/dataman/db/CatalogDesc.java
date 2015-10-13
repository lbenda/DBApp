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
package cz.lbenda.dataman.db;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 30.9.15.
 * Description of database catalog */
@SuppressWarnings("unused")
public class CatalogDesc implements Comparable<CatalogDesc> {

  private String name; public String getName() { return name; } public void setName(String name) { this.name = name; }

  private ObservableList<SchemaDesc> schemas = FXCollections.observableArrayList();
  public ObservableList<SchemaDesc> getSchemas() { return schemas; }
  private BooleanProperty hidden = new SimpleBooleanProperty(false);
  public boolean isHidden() { return hidden.get(); }
  public void setHidden(boolean hidden) { this.hidden.set(hidden); }

  private ChangeListener<Boolean> hiddenListener = (observable, oldValue, newValue) -> {
    if (!newValue) { setHidden(false); }
    else { setHidden(schemas.stream().allMatch(SchemaDesc::isHidden)); }
  };

  /** Return schema by name if exist */
  public SchemaDesc getSchema(@Nonnull String name) {
    synchronized (schemas) {
      List<SchemaDesc> sch = schemas.stream().filter(schema -> name.equals(schema.getName())).collect(Collectors.toList());
      return sch.size() == 0 ? null : sch.get(0);
    }
  }

  public CatalogDesc() {}
  public CatalogDesc(String name) {
    this.name = name;
    schemas.addListener((ListChangeListener<SchemaDesc>) change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          change.getAddedSubList().forEach(schema -> schema.hiddenProperty().addListener(hiddenListener));
        }
        if (change.wasRemoved()) {
          change.getRemoved().forEach(schema -> schema.hiddenProperty().removeListener(hiddenListener));
        }
      }
    });
  }

  @Override
  public int compareTo(@Nonnull CatalogDesc catalog) {
    if (getClass().equals(catalog.getClass())) {
      throw new ClassCastException("The input parameter have bad type: " + catalog.getClass());
    }
    return name.compareTo(catalog.name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (!(o instanceof CatalogDesc)) { return false; }
    CatalogDesc that = (CatalogDesc) o;
    return !(name != null ? !name.equals(that.name) : that.name != null);
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    return result;
  }

  @Override
  public String toString() {
    return name;
  }
}
