/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.rc.db.dialect;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public interface SQLDialect {

  public static final Set<SQLDialect> DIALECTS = new HashSet<>();

  boolean isForDriver(String driver);

  String tableCatalog();
  String tableSchema();
  String tableName();
  String tableType();
  String tableRemarks();

  String columnTableCatalog();
  String columnTableSchema();
  String columnTableName();
  String columnName();
  String columnDateType();
  String columnSize();
  String columnNullable();
  String columnAutoIncrement();
  String columnGenerated();
  String columnRemarsk();

  String pkColumnName();

  String fkMasterTableCatalog();
  String fkMasterTableSchema();
  String fkMasterTableName();
  String fkMasterColumnName();
  String fkSlaveTableCatalog();
  String fkSlaveTableSchema();
  String fkSlaveTableName();
  String fkSlaveColumnName();

}