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

/** Interface for type Auditor which is object which write audit information about changes in database
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 4.9.15. */
public interface Auditor {

  /** Write audit information as plain text.
   * @param user User which changed data
   * @param plainTextAudit String which is write to audit log */
  void writePlainTextAudit(String user, String plainTextAudit);
}
