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
import cz.lbenda.rcp.StatusHelper;
import cz.lbenda.rcp.localization.Message;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.function.Consumer;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 24.10.15.
 * Class which execute SQL */
public class SQLSExecutor {

  public interface SQLSExecutorConsumer {
    void addQueryResult(SQLQueryResult result);
    boolean isStopOnFirstError();
  }

  private static final Logger LOG = LoggerFactory.getLogger(SQLSExecutor.class);
  @Message
  public static final String TASK_NAME = "Execute sql";
  @Message
  public static final String STEP_FINISH = "SQL command finish";

  public static String[] splitSQLS(String sqls) {
    if (StringUtils.isBlank(sqls)) { return new String[0]; }
    String[] lines = sqls.split("\n");
    StringBuilder sb = new StringBuilder();
    for (String line : lines) { sb.append(line.trim()).append("\n"); }
    return sb.toString().split(";\n");
  }

  private DbConfig dbConfig;
  private SQLSExecutorConsumer sqlsExecutorConsumer;
  private Consumer<SQLSExecutor> consoleShower;
  public SQLSExecutor(@Nonnull DbConfig dbConfig, @Nonnull SQLSExecutorConsumer sqlsExecutorConsumer,
                      Consumer<SQLSExecutor> consoleShower) {
    this.dbConfig = dbConfig;
    this.sqlsExecutorConsumer = sqlsExecutorConsumer;
    this.consoleShower = consoleShower;
  }

  public void executeBlocking(@Nonnull String... sqls) {
    StatusHelper.getInstance().progressStart(this, TASK_NAME, sqls.length);
    int i = 0;
    for (String sql1 : sqls) {
      String sql = sql1.trim();
      i++;
      StatusHelper.getInstance().progressNextStep(this, i + ": " + sql, 0);
      SQLQueryResult sqlQueryResult = new SQLQueryResult();
      sqlQueryResult.setSql(sql);

      if (dbConfig.connectionProvider.isConnected()) {
        dbConfig.getConnectionProvider().onPreparedStatement(sql,
            tuple2 -> this.statementToSQLQueryResult(sqlQueryResult, tuple2));
        sqlsExecutorConsumer.addQueryResult(sqlQueryResult);
      }
      if (sqlQueryResult.getErrorMsg() != null && sqlsExecutorConsumer.isStopOnFirstError()) {
        break;
      }
    }
    StatusHelper.getInstance().progressFinish(this, STEP_FINISH);
  }

  public void execute(@Nonnull String... sqls) {
    if (dbConfig.getConnectionProvider().isConnected() || ConnectionProvider.notConnectedDialog(dbConfig)) {
      new Thread(() -> executeBlocking(sqls)).start();
    }
  }

  private void statementToSQLQueryResult(SQLQueryResult result, Tuple2<PreparedStatement, SQLException> tuple) {
    if (tuple.get2() != null) {
      result.setErrorMsg(tuple.get2().getMessage());
      LOG.debug(String.format("Problem with execute SQL '%s'", result.getSql()), tuple.get2());
      if (consoleShower != null) {
        consoleShower.accept(this);
      }
    } else {
      try {
        boolean ex = tuple.get1().execute();
        if (ex) {
          try (ResultSet rs = tuple.get1().getResultSet()) {
            ResultSetMetaData mtd = rs.getMetaData();
            SQLQueryRows sqlRows = new SQLQueryRows();
            sqlRows.setSQL(result.getSql());
            result.setSqlQueryRows(sqlRows);
            int columnCount = mtd.getColumnCount();
            ColumnDesc columns[] = new ColumnDesc[columnCount];
            for (int i = 1; i <= columnCount; i++) {
              columns[i - 1] = new ColumnDesc(mtd, i, dbConfig.getDialect());
            }
            sqlRows.getMetaData().setColumns(columns);
            while (rs.next()) {
              RowDesc row = RowDesc.createNewRow(sqlRows.getMetaData(), RowDesc.RowDescState.LOADED);
              for (ColumnDesc columnDesc : sqlRows.getMetaData().getColumns()) {
                row.loadInitialColumnValue(columnDesc, rs);
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
