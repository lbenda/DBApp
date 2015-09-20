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
package cz.lbenda.dataman.db.handler;

import cz.lbenda.dataman.rc.DbConfigFactory;
import cz.lbenda.dataman.rc.DbConfig;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import cz.lbenda.rcp.ribbon.MenuOptions;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 12.9.15.
 * Menu options for select db configuration */
@ActionConfig(
    category = "/DbConfig/connect",
    id = "cz.lbenda.dataman.db.homeMenu.SelectDbConfigMenuOptions",
    priority = 100,
    gui = @ActionGUIConfig(
        /* iconBase = "database.png", */
        displayName = @Message(id="ChooseDbConfig", msg="Select db configuration"),
        displayTooltip = @Message(id="ChooseDbConfig_tooltip", msg="Select database which you want beforeOpenInit or edit.")
    )
)
public class DbConfigMenuOptions implements MenuOptions<DbConfig> {

  @Override
  public ObservableList<DbConfig> getItems() { return DbConfigFactory.getConfigurations(); }
  @Override
  public String itemToString(DbConfig item) { return item.getId(); }
  @Override
  public DbConfig stringToItem(String name) { return DbConfigFactory.getConfiguration(name); }

  /** The holder to which is set session configuration values */
  private ObjectProperty<DbConfig> dbConfigObserver;

  public DbConfigMenuOptions(ObjectProperty<DbConfig> dbConfigObserver) {
    this.dbConfigObserver = dbConfigObserver;
  }

  @Override
  public boolean onSelect(DbConfig item) {
    dbConfigObserver.setValue(item);
    return true;
  }
}
