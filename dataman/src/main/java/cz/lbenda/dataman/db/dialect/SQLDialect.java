/*
 * Copyright 2015 Lukas Benda <lbenda at lbenda.cz>.
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

import java.util.HashSet;
import java.util.Set;

/** @author Lukas Benda <lbenda at lbenda.cz> */
public interface SQLDialect {

  Set<SQLDialect> DIALECTS = new HashSet<>();

  boolean isForDriver(String driver);

  default String tableCatalog() { return "TABLE_CAT"; }
  default String tableSchema() { return "TABLE_SCHEM"; }
  default String tableName()  { return "TABLE_NAME"; }
  default String tableType() { return "TABLE_TYPE"; }
  default String tableRemarks()  { return "REMARKS"; }

  default String columnTableCatalog() { return "TABLE_CAT"; }
  default String columnTableSchema()  { return "TABLE_SCHEM"; }
  default String columnTableName()  { return "TABLE_NAME"; }
  default String columnName() { return "COLUMN_NAME"; }
  default String columnDateType() { return "DATA_TYPE"; }
  default String columnTypeName() { return "TYPE_NAME"; }
  default String columnSize() { return "COLUMN_SIZE"; }
  default String columnNullable() { return "IS_NULLABLE"; }
  default String columnAutoIncrement() { return "IS_AUTOINCREMENT"; }
  default String columnGenerated() { return "IS_GENERATEDCOLUMN"; }
  default String columnRemarsk() { return "REMARKS"; }

  default String pkColumnName() { return "COLUMN_NAME"; }

  default String fkMasterTableCatalog() { return "PKTABLE_CAT"; }
  default String fkMasterTableSchema() { return "PKTABLE_SCHEM"; }
  default String fkMasterTableName() { return "PKTALBLE_NAME"; }
  default String fkMasterColumnName() { return "PKCOLUMN_NAME"; }
  default String fkSlaveTableCatalog() { return "FKTABLE_CAT"; }
  default String fkSlaveTableSchema() { return "FKTABLE_SCHEM"; }
  default String fkSlaveTableName() { return "FKTABLE_NAME"; }
  default String fkSlaveColumnName() { return "FKCOLUMN_NAME"; }

}