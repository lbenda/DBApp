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

import cz.lbenda.common.BitArrayBinaryData;
import cz.lbenda.dataman.Constants;
import cz.lbenda.dataman.db.dialect.ColumnType;
import cz.lbenda.dataman.db.dialect.MSSQLDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.UUID;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 20.9.15. */
public class TestExportTableData {

  private static final Logger LOG = LoggerFactory.getLogger(TestExportTableData.class);

  @Test
  public void saveToXLSX() {
    CatalogDesc catalogDesc = new CatalogDesc("catalog");
    SchemaDesc schemaDesc = new SchemaDesc(catalogDesc, "schema");
    catalogDesc.getSchemas().add(schemaDesc);
    TableDesc tableDesc = new TableDesc(schemaDesc, "TABLE", "table");
    ColumnDesc cd1 = new ColumnDesc(tableDesc, "id", null, ColumnType.INTEGER, 10, 0, false, true, true, "NULL");
    cd1.setPosition(1);
    ColumnDesc cd2 = new ColumnDesc(tableDesc, "column2", null, ColumnType.STRING, 250, 0, true, false, false, "NULL");
    cd2.setPosition(2);
    ColumnDesc cd3 = new ColumnDesc(tableDesc, "column3", null, ColumnType.DATE, 12, 0, true, false, false, "NULL");
    cd3.setPosition(3);
    ColumnDesc cd4 = new ColumnDesc(tableDesc, "column4", null, ColumnType.BOOLEAN, 1, 0, true, false, false, "NULL");
    cd4.setPosition(4);
    tableDesc.getQueryRow().getMetaData().getColumns().addAll(cd1, cd2, cd3, cd4);

    RowDesc row1 = tableDesc.addNewRowAction();
    row1.setColumnValue(cd1, 0);
    row1.setColumnValue(cd2, "varchar column 1");
    row1.setColumnValue(cd3, new Date((new java.util.Date()).getTime()));
    row1.setColumnValue(cd4, Boolean.TRUE);

    row1 = tableDesc.addNewRowAction();
    row1.setColumnValue(cd1, 1);
    row1.setColumnValue(cd2, "");
    row1.setColumnValue(cd3, new Date((new java.util.Date()).getTime()));
    row1.setColumnValue(cd4, Boolean.FALSE);

    row1 = tableDesc.addNewRowAction();
    row1.setColumnValue(cd1, 2);
    row1.setColumnValue(cd2, null);
    row1.setColumnValue(cd3, null);
    row1.setColumnValue(cd4, null);

    try {
      File file = File.createTempFile("TestExportTableData", ".XLSX");
      FileOutputStream fos = new FileOutputStream(file);
      ExportTableData.writeSqlQueryRowsToXLSX(tableDesc.getQueryRow(), Constants.EXPORT_SHEET_NAME, fos);
      fos.flush();
      fos.close();
    } catch (IOException e) {
      Assert.fail(e.toString());
    }
  }

  @Test
  public void saveToSQL() {
    CatalogDesc catalogDesc = new CatalogDesc("catalog");
    SchemaDesc schemaDesc = new SchemaDesc(catalogDesc, "schema");
    catalogDesc.getSchemas().add(schemaDesc);
    TableDesc tableDesc = new TableDesc(schemaDesc, "TABLE", "table");
    ColumnDesc cd1 = new ColumnDesc(tableDesc, "id", null, ColumnType.INTEGER, 10, 0, false, true, true, "NULL");
    cd1.setPosition(1);
    ColumnDesc cd2 = new ColumnDesc(tableDesc, "column2", null, ColumnType.STRING, 250, 0, true, false, false, "NULL");
    cd2.setPosition(2);
    ColumnDesc cd3 = new ColumnDesc(tableDesc, "column3", null, ColumnType.DATE, 12, 0, true, false, false, "NULL");
    cd3.setPosition(3);
    ColumnDesc cd4 = new ColumnDesc(tableDesc, "column4", null, ColumnType.TIME, 12, 0, true, false, false, "NULL");
    cd4.setPosition(4);
    ColumnDesc cd5 = new ColumnDesc(tableDesc, "column5", null, ColumnType.TIMESTAMP, 12, 0, true, false, false, "NULL");
    cd5.setPosition(5);
    ColumnDesc cd6 = new ColumnDesc(tableDesc, "column6", null, ColumnType.BOOLEAN, 1, 0, true, false, false, "NULL");
    cd6.setPosition(6);
    ColumnDesc cd7 = new ColumnDesc(tableDesc, "column7", null, ColumnType.DOUBLE, 17, 4, true, false, false, "NULL");
    cd7.setPosition(7);
    ColumnDesc cd8 = new ColumnDesc(tableDesc, "column8", null, ColumnType.DECIMAL, 17, 4, true, false, false, "NULL");
    cd8.setPosition(8);
    ColumnDesc cd9 = new ColumnDesc(tableDesc, "column9", null, ColumnType.BIT_ARRAY, 8, 0, true, false, false, "NULL");
    cd9.setPosition(9);
    ColumnDesc cd10 = new ColumnDesc(tableDesc, "column10", null, ColumnType.UUID, 32, 0, true, false, false, "NULL");
    cd10.setPosition(10);
    tableDesc.getQueryRow().getMetaData().getColumns().addAll(cd1, cd2, cd3, cd4, cd5, cd6, cd7, cd8, cd9, cd10);
    tableDesc.getQueryRow().setDialect(new MSSQLDialect());

    RowDesc row1 = tableDesc.addNewRowAction();
    row1.setColumnValue(cd1, 0);
    row1.setColumnValue(cd2, "varchar column 1");
    row1.setColumnValue(cd3, new Date((new java.util.Date()).getTime()));
    row1.setColumnValue(cd4, new Time((new java.util.Date()).getTime()));
    row1.setColumnValue(cd5, new Timestamp((new java.util.Date()).getTime()));
    row1.setColumnValue(cd6, Boolean.TRUE);
    row1.setColumnValue(cd7, 15.5d);

    row1 = tableDesc.addNewRowAction();
    row1.setColumnValue(cd1, 1);
    row1.setColumnValue(cd2, "");
    row1.setColumnValue(cd3, new Date((new java.util.Date()).getTime()));
    row1.setColumnValue(cd6, Boolean.FALSE);
    row1.setColumnValue(cd7, 123456789.1234d);

    row1 = tableDesc.addNewRowAction();
    row1.setColumnValue(cd1, 2);
    row1.setColumnValue(cd2, null);
    row1.setColumnValue(cd3, null);
    row1.setColumnValue(cd6, null);
    row1.setColumnValue(cd7, 123456789d);

    row1 = tableDesc.addNewRowAction();
    row1.setColumnValue(cd1, 3);
    row1.setColumnValue(cd7, 0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001234567890123456789012345678d);

    row1 = tableDesc.addNewRowAction();
    row1.setColumnValue(cd1, 4);
    row1.setColumnValue(cd8, new BigDecimal("15671.1536"));

    row1 = tableDesc.addNewRowAction();
    row1.setColumnValue(cd1, 5);
    row1.setColumnValue(cd9, new BitArrayBinaryData("column9", new byte[] { (byte) 165 }));

    row1 = tableDesc.addNewRowAction();
    row1.setColumnValue(cd1, 6);
    row1.setColumnValue(cd10, UUID.randomUUID());

    StringWriter writer = new StringWriter();
    ExportTableData.writeSqlQueryRowsToSQL(tableDesc.getQueryRow(), writer);
    System.out.println("Exported string: ");
    System.out.println(writer.toString());
    /*
    String[] sqls = SQLSExecutor.splitSQLS(writer.toString());
    SQLSExecutor sqlsExecutor = new
     */
  }
}
