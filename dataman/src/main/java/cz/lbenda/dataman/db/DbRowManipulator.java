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

import cz.lbenda.dataman.db.audit.Auditor;
import cz.lbenda.dataman.db.audit.AuditorNone;
import cz.lbenda.dataman.db.audit.SqlLogToLogAuditor;
import cz.lbenda.dataman.db.audit.SqlLogToTableAuditor;
import cz.lbenda.dataman.db.dialect.ColumnType;
import cz.lbenda.dataman.schema.exconf.AuditType;
import cz.lbenda.dataman.schema.exconf.AuditTypeType;
import cz.lbenda.rcp.ExceptionMessageFrmController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 27.9.15.
 * Object which manipulate with rows in database */
public class DbRowManipulator {

  private static final Logger LOG = LoggerFactory.getLogger(DbRowManipulator.class);

  private final ObjectProperty<ConnectionProvider> connectionProvider = new SimpleObjectProperty<>();

  public DbRowManipulator(ConnectionProvider connectionProvider) {
    this.connectionProvider.setValue(connectionProvider);
  }

  @SuppressWarnings("unused")
  public void setConnectionProvider(@Nonnull ConnectionProvider connectionProvider) { this.connectionProvider.setValue(connectionProvider); }
  @SuppressWarnings("unused")
  public ConnectionProvider getConnectionProvider() { return this.connectionProvider.getValue(); }
  @SuppressWarnings("unused")
  public ObjectProperty<ConnectionProvider> connectionProvider() { return connectionProvider; }

  /** Save all changes inside table which is given as paramte */
  public void saveChanges(@Nonnull TableDesc td) {
    List<RowDesc> deleteRows = td.getRows().stream().filter(row -> RowDesc.RowDescState.REMOVED.equals(row.getState())).collect(Collectors.toList());
    List<RowDesc> updateRows = td.getRows().stream().filter(row -> RowDesc.RowDescState.CHANGED.equals(row.getState())).collect(Collectors.toList());
    List<RowDesc> insertRows = td.getRows().stream().filter(row -> RowDesc.RowDescState.NEW.equals(row.getState())).collect(Collectors.toList());
    if (!deleteRows.isEmpty() || !updateRows.isEmpty() || !insertRows.isEmpty()) {
      try (Connection connection = getConnectionProvider().getConnection()) {
        boolean autocommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
          insertRows(connection, td, insertRows);
          updateRows(connection, td, updateRows);
          deleteRows(connection, td, deleteRows);
          connection.commit();
          td.getRows().removeAll(deleteRows);
          insertRows.forEach(RowDesc::savedChanges);
          updateRows.forEach(RowDesc::savedChanges);
        } catch (SQLException e) {
          connection.rollback();
          LOG.error("Problem with save inserted/update/delete row", e);
          ExceptionMessageFrmController.showException(e);
        }
        connection.setAutoCommit(autocommit);
      } catch (SQLException e) {
        LOG.error("Problem with getting and configure connection", e);
        ExceptionMessageFrmController.showException(e);
      }
    }
  }

  /** Insert given rows to database */
  private void insertRows(Connection connection, TableDesc td, List<RowDesc> rows) throws SQLException {
    final List<ColumnDesc> pks = td.getPKColumns().stream().filter(col -> col.isGenerated() || col.isAutoincrement()).collect(Collectors.toList());
    for (RowDesc row : rows) {
      StringBuilder names = new StringBuilder();
      StringBuilder values = new StringBuilder();
      List<ColumnDesc> insertedColumns = new ArrayList<>();
      td.getColumns().stream().filter(col -> !col.isGenerated() && row.isColumnChanged(col)).forEach(col -> {
        if (!insertedColumns.isEmpty()) {
          names.append(", ");
          values.append(", ");
        }
        insertedColumns.add(col);
        names.append('"').append(col.getName()).append("\"");
        values.append('?');
      });

      String sql = String.format("insert into \"%s\".\"%s\" (%s) values (%s)", td.getSchema(), td.getName(), names, values);
      LOG.trace(sql);

      try (PreparedStatement ps = AuditPreparedStatement.prepareStatement(getConnectionProvider().getUser(),
          auditorForAudit(td.getAudit()), connection, sql, Statement.RETURN_GENERATED_KEYS)) {
        int i = 1;
        for (ColumnDesc col : insertedColumns) {
          row.putValueToPS(col, ps, i);
          i++;
        }
        ps.execute();
        if (!pks.isEmpty()) {
          ResultSet rs = ps.getGeneratedKeys();
          ResultSetMetaData rsmd = rs.getMetaData();
          while (rs.next()) {
            for (int j = 1; j <= rsmd.getColumnCount(); j++) {
              ColumnDesc col = td.getColumn(rsmd.getColumnName(j));
              if (col == null) {
                if (getConnectionProvider().getDialect().nameOfGeneratedIdentityColumn().contains(rsmd.getColumnName(j))) {
                  col = td.getPKColumns().get(0);
                } else {
                  throw new IllegalStateException(String.format("The column with name %s not exist", rsmd.getColumnName(j)));
                }
              }
              row.setInitialColumnValue(col, rs.getObject(j)); // Set value of primary key, so this data is LOADED not changed
            }
          }
        }
        LOG.debug("New column was inserted");
      }
    }
  }

  /** Return list of primary kolumns, if no primary columns is for table defined, then it's use all usable column for
   * recognise row */
  private List<ColumnDesc> getPrimaryKeys(TableDesc td) {
    List<ColumnDesc> result = td.getPKColumns();
    if (result.isEmpty()) {
      result = td.getColumns().stream().filter(col ->
              col.getDataType() != ColumnType.BLOB
                  && col.getDataType() != ColumnType.CLOB
                  && col.getDataType() != ColumnType.ARRAY
                  && col.getDataType() != ColumnType.BYTE_ARRAY
                  && col.getDataType() != ColumnType.OBJECT
      ).collect(Collectors.toList());
    }
    return result;
  }

  private void updateRows(Connection connection, TableDesc td, List<RowDesc> rows) throws SQLException {
    final List<ColumnDesc> pks = getPrimaryKeys(td);
    final StringBuilder where = new StringBuilder();
    pks.stream().forEachOrdered(col -> {
      if (where.length() != 0) { where.append(" and "); }
      where.append('"').append(col.getName()).append("\"=?");
    });
    for (RowDesc row : rows) {
      final StringBuilder set = new StringBuilder();
      final List<ColumnDesc> changedColumns = td.getColumns().stream().filter(col -> !col.isGenerated()
          && row.isColumnChanged(col)).collect(Collectors.toList());

      changedColumns.forEach(col -> {
        if (set.length() > 0) { set.append(", "); }
        set.append('"').append(col.getName()).append("\"=?");
      });
      String sql = String.format("update \"%s\".\"%s\" set %s where %s", td.getSchema(), td.getName(), set, where);
      LOG.trace(sql);

      try (PreparedStatement ps = AuditPreparedStatement.prepareCall(getConnectionProvider().getUser(),
          auditorForAudit(td.getAudit()), connection, sql)) {
        int i = 1;
        for (ColumnDesc columnDesc : changedColumns) {
          row.putValueToPS(columnDesc, ps, i);
          i++;
        }
        for (ColumnDesc col : pks) {
          row.putValueToPS(col, ps, i);
          i++;
        }
        ps.execute();
      }
    }
  }

  /** Method which remove row from table. If table have primary key, then use primary key for delete row, if haven't
   * then use values of all columns. If there is table without any unique index (or primary key) then can be more then
   * one row deleted.
   * @param td table description */
  private void deleteRows(Connection connection, TableDesc td, List<RowDesc> rows) throws SQLException {
    final List<ColumnDesc> pks = getPrimaryKeys(td);
    final StringBuilder where = new StringBuilder();
    pks.forEach(col -> {
      if (where.length() != 0) {
        where.append(" and ");
      }
      where.append('"').append(col.getName()).append('"').append("=?");
    });
    String sql = String.format("DELETE FROM \"%s\".\"%s\" WHERE %s", td.getSchema(), td.getName(), where);
    LOG.trace(sql);
    for (RowDesc row : rows) {
      try (PreparedStatement ps = AuditPreparedStatement.prepareCall(getConnectionProvider().getUser(),
          auditorForAudit(td.getAudit()), connection, sql)) {
        int i = 1;
        for (ColumnDesc col : pks) {
          row.putInitialValueToPS(col, ps, i);
          i++;
        }
        ps.execute();
      }
    }
  }

  /** Return auditor object for given audit type with configuration
   * @param auditType audit configuration
   * @return auditor, never return null */
  private Auditor auditorForAudit(AuditType auditType) {
    switch (auditType == null || auditType.getType() == null ? AuditTypeType.NONE : auditType.getType()) {
      case NONE : return AuditorNone.getInstance();
      case SQL_LOG_TO_LOG: return SqlLogToLogAuditor.getInstance();
      case SQL_LOG_TO_TABLE : return SqlLogToTableAuditor.getInstance(getConnectionProvider(), auditType);
      default : return AuditorNone.getInstance();
    }
  }
}
