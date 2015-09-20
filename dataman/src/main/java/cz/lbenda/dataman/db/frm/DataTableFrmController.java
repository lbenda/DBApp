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
package cz.lbenda.dataman.db.frm;

import cz.lbenda.dataman.db.*;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 10.9.15.
 * Controller for showing data in table */
public class DataTableFrmController {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(DataTableFrmController.class);

  private DataTableView tableView;
  private final ObjectProperty<String> title = new SimpleObjectProperty<>();

  public DataTableFrmController(TableDesc td) {
    tableView = new DataTableView(td);
    td.dirtyStateProperty().addListener((observable, oldValue, newValue) -> {
      title.setValue(generateTitle(td));
    });
    title.setValue(generateTitle(td));
    td.reloadRowsAction();
    this.tableView.setMetaData(td.getQueryRow().getMetaData());
    this.tableView.setRows(td.getRows());
  }

  private String generateTitle(TableDesc td) {
    if (Boolean.TRUE.equals(td.dirtyStateProperty().getValue())) {
      return "* " + td.getName();
    }
    return td.getName();
  }

  public DataTableFrmController(SQLQueryResult sqlQueryResult) {
    tableView = new DataTableView(null);
    title.setValue(sqlQueryResult.getSql());
    this.tableView.setMetaData(sqlQueryResult.getSqlQueryRows().getMetaData());
    this.tableView.setRows(sqlQueryResult.getSqlQueryRows().getRows());
  }

  /** Return parent node which hold the controller */
  public DataTableView getTabView() {
    return tableView;
  }

  public ObjectProperty<String> titleProperty() { return title; }
}
