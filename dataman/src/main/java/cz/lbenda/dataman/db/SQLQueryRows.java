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

import cz.lbenda.dataman.db.dialect.SQLDialect;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 13.9.15.
 * Result from SQL query which return rows */
public class SQLQueryRows {
  /** Dialect which rows extract */
  private SQLDialect dialect; public SQLDialect getDialect() { return dialect; } public void setDialect(SQLDialect dialect) { this.dialect = dialect; }
  /** SQL which was executed for getting this data */
  private String sql; public String getSQL() { return sql; } public void setSQL(String sql) { this.sql = sql; }
  /** Metadata for table and columns in rows */
  private SQLQueryMetaData metaData = new SQLQueryMetaData(); public SQLQueryMetaData getMetaData() { return metaData; }
  /** List of rows in result of SQL query */
  private ObservableList<RowDesc> rows = FXCollections.observableArrayList(); public ObservableList<RowDesc> getRows() { return rows; }
}
