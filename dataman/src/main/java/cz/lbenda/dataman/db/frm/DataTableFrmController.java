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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 10.9.15.
 * Controller for showing data in table */
public class DataTableFrmController {

  private static final Logger LOG = LoggerFactory.getLogger(DataTableFrmController.class);
  private DataTableView tableView;

  private static Map<TableDesc, DataTableFrmController> controllers = new HashMap<>();

  public DataTableFrmController(TableDesc td) {
    tableView = new DataTableView(td);
    td.reloadRowsAction();
    this.tableView.setMetaData(td.getQueryRow().getMetaData());
    this.tableView.setRows(td.getRows());
  }

  public DataTableFrmController(SQLQueryResult sqlQueryResult) {
    tableView = new DataTableView(null);
    this.tableView.setMetaData(sqlQueryResult.getSqlQueryRows().getMetaData());
    this.tableView.setRows(sqlQueryResult.getSqlQueryRows().getRows());
  }

  /** Return parent node which hold the controller */
  public DataTableView getTabView() {
    return tableView;
  }

}
