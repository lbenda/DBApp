/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dataman.db.dialect;

/**
 *
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public class HSQLDBDialect implements SQLDialect {

  static {
    SQLDialect.DIALECTS.add(new HSQLDBDialect());
  }

  @Override
  public boolean isForDriver(String driver) {
    if (driver == null) { return false; }
    return driver.startsWith("org.hsqldb");
  }

  @Override
  public String tableCatalog() {
    return "TABLE_CAT";
  }

  @Override
  public String tableSchema() {
    return "TABLE_SCHEM";
  }

  @Override
  public String tableName() {
    return "TABLE_NAME";
  }

  @Override
  public String tableType() {
    return "TABLE_TYPE";
  }

  @Override
  public String tableRemarks() {
    return "REMARKS";
  }

  @Override
  public String columnTableCatalog() {
    return "TABLE_CAT";
  }

  @Override
  public String columnTableSchema() {
    return "TABLE_SCHEM";
  }

  @Override
  public String columnTableName() {
    return "TABLE_NAME";
  }

  @Override
  public String columnName() {
    return "COLUMN_NAME";
  }

  @Override
  public String columnDateType() {
    return "DATA_TYPE";
  }

  @Override
  public String columnSize() {
    return "COLUMN_SIZE";
  }

  @Override
  public String columnNullable() {
    return "IS_NULLABLE";
  }

  @Override
  public String columnAutoIncrement() {
    return "IS_AUTOINCREMENT";
  }

  @Override
  public String columnGenerated() {
    return "IS_GENERATEDCOLUMN";
  }

  @Override
  public String columnRemarsk() {
    return "REMARSKS";
  }

  @Override
  public String pkColumnName() {
    return "COLUMN_NAME";
  }

  @Override
  public String fkMasterTableCatalog() {
    return "PKTABLE_CAT";
  }

  @Override
  public String fkMasterTableSchema() {
    return "PKTABLE_SCHEM";
  }

  @Override
  public String fkMasterTableName() {
    return "PKTALBLE_NAME";
  }

  @Override
  public String fkMasterColumnName() {
    return "PKCOLUMN_NAME";
  }

  @Override
  public String fkSlaveTableCatalog() {
    return "FKTABLE_CAT";
  }

  @Override
  public String fkSlaveTableSchema() {
    return "FKTABLE_SCHEM";
  }

  @Override
  public String fkSlaveTableName() {
    return "FKTABLE_NAME";
  }

  @Override
  public String fkSlaveColumnName() {
    return "FKCOLUMN_NAME";
  }

}
