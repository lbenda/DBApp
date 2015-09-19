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

import cz.lbenda.dataman.db.frm.ConnectedTablesFrmController;
import cz.lbenda.dataman.db.frm.DataTableView;
import cz.lbenda.dataman.rc.DetailDescriptor;
import cz.lbenda.rcp.action.AbstractAction;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import cz.lbenda.rcp.localization.MessageFactory;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * beforeOpenInit slaves tables */
@ActionConfig(
    category = "/SQL/details",
    id = "cz.lbenda.dataman.db.handler.OpenSlaveTableHandler",
    priority = 300,
    gui = {
        @ActionGUIConfig(
            iconBase = "slave-table.png",
            displayName = @Message(id="Slave_table", msg="Slaves"),
            displayTooltip = @Message(id="Slave_table_tooltip", msg="Open slave tables")
        ),
        @ActionGUIConfig(
            iconBase = "table.png",
            displayName = @Message(id="Master_table", msg="Slaves"),
            displayTooltip = @Message(id="Master_table_tooltip", msg="Hide slave tables")
        )
    }
)
public class OpenConnectedTablesHandler extends AbstractAction {

  public Map<DataTableView, Node> showedDetailTables = new WeakHashMap<>();

  private static Logger LOG = LoggerFactory.getLogger(OpenConnectedTablesHandler.class);

  @Message
  private String msgDetailTitle = "Connected tables";

  private ObjectProperty<DataTableView> tableViewObjectProperty;
  private Consumer<DetailDescriptor> detailAppender;

  public OpenConnectedTablesHandler(ObjectProperty<DataTableView> tableViewObjectProperty,
                                    Consumer<DetailDescriptor> detailAppender) {
    MessageFactory.initializeMessages(this);
    this.tableViewObjectProperty = tableViewObjectProperty;
    this.detailAppender = detailAppender;
    tableViewObjectProperty.addListener((observable, oldValue, newValue) -> {
      setConfig(showedDetailTables.containsKey(tableViewObjectProperty.get()) ? 1 : 0);
      closeDetailView(oldValue);
      if (showedDetailTables.containsKey(newValue)) {
        openDetailView(newValue);
      } else {
        closeDetailView(newValue);
      }
      setEnable(tableViewObjectProperty.get() != null);
    });
  }

  public void openDetailView(DataTableView dt) {
    if (!showedDetailTables.containsKey(dt)) {
      showedDetailTables.put(dt, (new ConnectedTablesFrmController(dt)).getMainPane());
    }
    detailAppender.accept(new DetailDescriptor(msgDetailTitle, showedDetailTables.get(dt), true));
  }

  public void closeDetailView(DataTableView dt) {
    if (showedDetailTables.containsKey(dt)) {
      detailAppender.accept(new DetailDescriptor(msgDetailTitle, showedDetailTables.get(dt), true));
    }
  }

  @Override
  public void handle(ActionEvent e) {
    if (showedDetailTables.containsKey(tableViewObjectProperty.getValue())) {
      setConfig(0);
      closeDetailView(tableViewObjectProperty.getValue());
      showedDetailTables.remove(tableViewObjectProperty.getValue());
    } else {
      setConfig(1);
      openDetailView(tableViewObjectProperty.getValue());
    }
  }
}