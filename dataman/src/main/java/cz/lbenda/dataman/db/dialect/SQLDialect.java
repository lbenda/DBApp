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

import java.sql.Types;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** @author Lukas Benda <lbenda at lbenda.cz> */
@SuppressWarnings("unused")
public interface SQLDialect {

  Set<SQLDialect> DIALECTS = new HashSet<>();

  boolean isForDriver(String driver);

  default byte incrementFrom() { return 0; }

  default String tableCatalog() { return "TABLE_CAT"; }
  default String tableSchema() { return "TABLE_SCHEM"; }
  default String tableName() { return "TABLE_NAME"; }
  default String tableType() { return "TABLE_TYPE"; }
  default String tableRemarks()  { return "REMARKS"; }

  default String columnTableCatalog() { return "TABLE_CAT"; }
  default String columnTableSchema()  { return "TABLE_SCHEM"; }
  default String columnTableName()  { return "TABLE_NAME"; }
  default String columnName() { return "COLUMN_NAME"; }
  default String columnDefaultValue() { return "COLUMN_DEF"; }
  default String columnDateType() { return "DATA_TYPE"; }
  default String columnTypeName() { return "TYPE_NAME"; }
  default String columnSize() { return "COLUMN_SIZE"; }
  default String columnDecimalDigits() { return "DECIMAL_DIGITS"; }
  default String columnNumPrecRadix() { return "NUM_PREC_RADIX"; }
  default String columnNullable() { return "IS_NULLABLE"; }
  default String columnAutoIncrement() { return "IS_AUTOINCREMENT"; }
  default String columnGenerated() { return "IS_GENERATEDCOLUMN"; }
  default String columnRemarks() { return "REMARKS"; }

  default String pkColumnName() { return "COLUMN_NAME"; }

  default String fkName() { return "FK_NAME"; }
  default String fkMasterTableCatalog() { return "PKTABLE_CAT"; }
  default String fkMasterTableSchema() { return "PKTABLE_SCHEM"; }
  default String fkMasterTableName() { return "PKTALBLE_NAME"; }
  default String fkMasterColumnName() { return "PKCOLUMN_NAME"; }
  default String fkSlaveTableCatalog() { return "FKTABLE_CAT"; }
  default String fkSlaveTableSchema() { return "FKTABLE_SCHEM"; }
  default String fkSlaveTableName() { return "FKTABLE_NAME"; }
  default String fkSlaveColumnName() { return "FKCOLUMN_NAME"; }
  default String fkUpdateRule() { return "UPDATE_RULE"; }
  default String fkDeleteRule() { return "DELETE_RULE"; }

  /** Set of names which is used to describe identity column */
  default Set<String> nameOfGeneratedIdentityColumn() { return new HashSet<>(Arrays.asList(new String[] {
      "SCOPE_IDENTITY()", "IDENTITY()", "1" })); }

  default ColumnType columnTypeFromSQL(int dataType, String columnTypeName, int size) {
    switch (dataType) {
      case Types.TIMESTAMP : return ColumnType.TIMESTAMP;
      case Types.TIME : return ColumnType.TIME;
      case Types.DATE : return ColumnType.DATE;
      case Types.BIT :
        if (size == 1 ) { return ColumnType.BIT; }
        return ColumnType.BIT_ARRAY;
      case Types.INTEGER : return ColumnType.INTEGER;
      case Types.BIGINT : return ColumnType.LONG;
      case Types.TINYINT : return ColumnType.BYTE;
      case Types.SMALLINT : return ColumnType.SHORT;
      case Types.FLOAT :
      case Types.REAL : return ColumnType.FLOAT;
      case Types.NUMERIC :
      case Types.DECIMAL : return ColumnType.DECIMAL;
      case Types.DOUBLE : return ColumnType.DOUBLE;
      case Types.CHAR :
      case Types.VARCHAR : return ColumnType.STRING;
      case Types.BOOLEAN : return ColumnType.BOOLEAN;
      case Types.VARBINARY: return ColumnType.BYTE_ARRAY;
      case Types.BINARY :
        if ("UUID".equals(columnTypeName)) { return ColumnType.UUID; }
        else { return ColumnType.BYTE_ARRAY; }
      case Types.CLOB : return ColumnType.CLOB;
      case Types.BLOB : return ColumnType.BLOB;
      case Types.ARRAY : return ColumnType.ARRAY;
      case Types.OTHER :
      default : return ColumnType.OBJECT;
    }
  }
}