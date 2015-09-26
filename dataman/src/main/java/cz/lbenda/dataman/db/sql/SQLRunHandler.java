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

import cz.lbenda.common.Tuple2;
import cz.lbenda.dataman.db.*;
import cz.lbenda.dataman.rc.DbConfig;
import cz.lbenda.rcp.action.AbstractAction;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Action which run SQL command */
@ActionConfig(
    category = "/SQL/sql",
    id = "cz.lbenda.dataman.db.sql.SQLRunHandler",
    priority = 100,
    gui = @ActionGUIConfig(
      displayName = @Message(id="Run", msg="Run"),
      displayTooltip = @Message(id="Run_tooltip", msg="Run sql commands which is selected or whole content of editor."),
      iconBase = "sqlRun.png"
    )
)
public class SQLRunHandler extends AbstractAction {

  private static Logger LOG = LoggerFactory.getLogger(SQLRunHandler.class);
  private SQLEditorController sqlEditorController;
  private ObjectProperty<DbConfig> dbConfigProperty;

  public SQLRunHandler(ObjectProperty<DbConfig> dbConfigProperty, SQLEditorController sqlEditorController) {
    this.sqlEditorController = sqlEditorController;
    this.dbConfigProperty = dbConfigProperty;
    dbConfigProperty.addListener(observable ->
      setEnable(dbConfigProperty.getValue() != null && dbConfigProperty.getValue().isConnected()));
  }

  @Override
  public void handle(ActionEvent e) {
    String[] sqls = sqlEditorController.getExecutedText();

    for (String sql : sqls) {
      SQLQueryResult sqlQueryResult = new SQLQueryResult();
      sqlQueryResult.setSql(sql);
      if (dbConfigProperty.getValue() != null
          && dbConfigProperty.getValue().isConnected()) {
        dbConfigProperty.getValue().getReader().onPreparedStatement(sql,
            tuple2 -> this.statementToSQLQueryResult(sqlQueryResult, tuple2));
        sqlEditorController.addQueryResult(sqlQueryResult);
      }
    }
  }

  public void statementToSQLQueryResult(SQLQueryResult result, Tuple2<PreparedStatement, SQLException> tuple) {
    if (tuple.get2() != null) {
      result.setErrorMsg(tuple.get2().getMessage());
      LOG.debug(String.format("Problem with execute SQL '%s'", result.getSql()), tuple.get2());
    } else {
      try {
        boolean ex = tuple.get1().execute();
        if (ex) {
          try (ResultSet rs = tuple.get1().getResultSet()) {
            ResultSetMetaData mtd = rs.getMetaData();
            SQLQueryRows sqlRows = new SQLQueryRows();
            result.setSqlQueryRows(sqlRows);
            int columnCount = mtd.getColumnCount();
            ColumnDesc columns[] = new ColumnDesc[columnCount];
            for (int i = 1; i <= columnCount; i++) {
              columns[i - 1] = new ColumnDesc(mtd, i);
            }
            sqlRows.getMetaData().setColumns(columns);
            while (rs.next()) {
              RowDesc row = RowDesc.createNewRow(sqlRows.getMetaData(), RowDesc.RowDescState.LOADED);
              for (ColumnDesc columnDesc : sqlRows.getMetaData().getColumns()) {
                row.setInitialColumnValue(columnDesc, rs.getObject(columnDesc.getPosition()));
              }
              sqlRows.getRows().add(row);
            }
          }
        } else {
          result.setAffectedRow(tuple.get1().getUpdateCount());
        }
      } catch (SQLException e) {
        result.setErrorMsg(e.getMessage());
        LOG.debug(String.format("Problem with execute SQL '%s'", result.getSql()), e);
      }
    }
  }
}
