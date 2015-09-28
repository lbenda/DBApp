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
package cz.lbenda.dataman.db.dialect;

import java.sql.Types;

/** @author Lukas Benda <lbenda at lbenda.cz> */
public class HSQLDBDialect implements SQLDialect {

  @Override
  public boolean isForDriver(String driver) {
    return driver != null && driver.startsWith("org.hsqldb");
  }

  public ColumnType columnTypeFromSQL(int dataType, String columnTypeName, int size) {
    switch (dataType) {
      case Types.TINYINT :
      case Types.SMALLINT :
        return ColumnType.INTEGER;
      default: return SQLDialect.super.columnTypeFromSQL(dataType, columnTypeName, size);
    }
  }
}
