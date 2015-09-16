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
package cz.lbenda.dataman.db.audit;

import cz.lbenda.dataman.db.DbStructureReader;
import cz.lbenda.dataman.schema.exconf.AuditType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.WeakHashMap;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 4.9.15.
 * Auditor which write log to database */
public class SqlLogToTableAuditor implements Auditor {

  private static final Map<AuditType, SqlLogToTableAuditor> CACHED_AUDITORS = new WeakHashMap<>();

  private static Logger LOG = LoggerFactory.getLogger(SqlLogToTableAuditor.class);
  private static String SQL_INSERT_LOG = "insert into %s (usr, created, log) values (?, ?, ?)";

  private final DbStructureReader dbStructureReader;
  private final AuditType auditType;

  public SqlLogToTableAuditor(DbStructureReader dbStructureReader, AuditType auditType) {
    this.dbStructureReader = dbStructureReader;
    this.auditType = auditType;
  }

  @Override
  public void writePlainTextAudit(String user, String plainTextAudit) {
    String tableName = "";
    if (auditType.getTargetLogTable().getCatalog() != null
        && !"".equals(auditType.getTargetLogTable().getCatalog().trim())) {
      tableName = auditType.getTargetLogTable().getCatalog() + ".";
    }
    tableName += auditType.getTargetLogTable().getSchema() + "." + auditType.getTargetLogTable().getTable();

    try (Connection conn = dbStructureReader.getConnection()) {
      try (PreparedStatement ps = conn.prepareCall(String.format(SQL_INSERT_LOG, tableName))) {
        ps.setString(1, dbStructureReader.getUser().getUsername());
        ps.setTimestamp(2, new Timestamp((new Date()).getTime()));
        ps.setString(3, plainTextAudit);
        ps.execute();
      } catch (SQLException e) {
        LOG.error("Problem with getting prepared statement for log", e);
      }
    } catch (SQLException e) {
      LOG.error("Problem with creating connection for log", e);
    }
  }

  public static SqlLogToTableAuditor getInstance(DbStructureReader dbStructureReader, AuditType auditType) {
    synchronized (CACHED_AUDITORS) {
      SqlLogToTableAuditor result = CACHED_AUDITORS.get(auditType);
      if (result == null) {
        result = new SqlLogToTableAuditor(dbStructureReader, auditType);
        CACHED_AUDITORS.put(auditType, result);
      }
      return result;
    }
  }
}
