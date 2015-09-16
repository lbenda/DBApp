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

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 7.9.15.
 * Object which hold deteched data from SQL query */
public class SQLQueryResult {
  private String sql; public String getSql() { return sql; } public void setSql(String sql) { this.sql = sql; }
  private String msg; public String getMsg() { return msg; } public void setMsg(String msg) { this.msg = msg; }
  private String errorMsg; public String getErrorMsg() { return errorMsg; } public void setErrorMsg(String error) { this.errorMsg = error; }
  private SQLQueryRows sqlQueryRows; public SQLQueryRows getSqlQueryRows() { return sqlQueryRows; } public void setSqlQueryRows(SQLQueryRows sqlQueryRows) { this.sqlQueryRows = sqlQueryRows; }
  private Integer affectedRow; public Integer getAffectedRow() { return affectedRow; } public void setAffectedRow(Integer affectedRow) { this.affectedRow = affectedRow; }
}
