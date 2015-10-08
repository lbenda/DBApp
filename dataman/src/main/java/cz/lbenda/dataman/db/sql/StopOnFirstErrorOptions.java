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
package cz.lbenda.dataman.db.sql;

import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import cz.lbenda.rcp.ribbon.MenuOptions;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 8.10.15.
 * Handler which switch if executing SQL fail on error */
@ActionConfig(
    category = "/SQL/sql",
    id = "cz.lbenda.dataman.db.sql.FailOnFirstErrorOptions",
    priority = 200,
    gui = @ActionGUIConfig(
        /* iconBase = "database.png", */
        displayName = @Message(id="StopOnFirstError", msg="Stop on first error"),
        displayTooltip = @Message(id="StopOnFirstError_tooltip", msg="Stop on with executing SQL on first error.")
    )
)
public class StopOnFirstErrorOptions implements MenuOptions<Boolean> {

  private final ObservableList<Boolean> items = FXCollections.observableArrayList();
  private final ObjectProperty<Boolean> stopOnFirstError;

  public StopOnFirstErrorOptions(ObjectProperty<Boolean> value) {
    items.addAll(true, false);
    this.stopOnFirstError = value;
  }

  @Override
  public ObservableList<Boolean> getItems() { return items; }
  @Override
  public String itemToString(Boolean item) { return String.valueOf(item); }
  @Override
  public Boolean stringToItem(String name) { return Boolean.valueOf(name); }
  @Override
  public void setSelect(Boolean item) { stopOnFirstError.setValue(item); }
  @Override
  public Boolean getSelect() { return stopOnFirstError.getValue(); }
  @Override
  public ObjectProperty<Boolean> selectProperty() { return stopOnFirstError; }

  @Override
  public void handle(ActionEvent stage) {}

  @Override
  public boolean isCheckBox() { return true; }
}
