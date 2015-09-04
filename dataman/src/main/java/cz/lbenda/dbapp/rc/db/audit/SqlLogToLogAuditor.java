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
package cz.lbenda.dbapp.rc.db.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Auditor which write SQL log into the log
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 4.9.15. */
public class SqlLogToLogAuditor implements Auditor {

  private static Logger LOG = LoggerFactory.getLogger(SqlLogToLogAuditor.class);

  public static final SqlLogToLogAuditor SQL_LOG_TO_LOG_AUDITOR = new SqlLogToLogAuditor();

  /** Write audit information as plain text.
   * @param user User which changed data
   * @param plainTextAudit String which is write to audit log */
  public void writePlainTextAudit(String user, String plainTextAudit) {
    LOG.debug(user + ": " + plainTextAudit);
  }

  public static final SqlLogToLogAuditor getInstance() { return SQL_LOG_TO_LOG_AUDITOR; }
}
