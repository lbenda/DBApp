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

import cz.lbenda.dataman.rc.DbConfig;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 19.9.15.
 * Testing implementation of data structure reader
 */
@SuppressWarnings("ConstantConditions")
public class TestDataStructureReader extends TestAbstractDB {

  @Test(dataProviderClass = TestAbstractDB.class, dataProvider = "databases", groups = "database")
  public void readStructureFromDatabase(TestHelperPrepareDB.DBDriver driverClass, String url, String catalog) {
    DbConfig config = TestHelperPrepareDB.createConfig(driverClass, url);
    config.getReader().generateStructure();
    // writeStructure(config); // Could be uncommented for seeing read struct

    assertNotNull(config.getSchemas(catalog));
    assertTrue(config.getSchemas(catalog).contains("test"));
    assertNotNull(config.getTableDescription(catalog, "test", "TABLE1"));
    assertTrue(config.getTableDescription(catalog, "test", "TABLE1").getColumn("ID").isAutoincrement());
    assertTrue(config.getTableDescription(catalog, "test", "TABLE1").getColumn("ID").isPK());

    assertFalse(config.getTableDescription(catalog, "test", "TABLE2").getColumn("ID").isAutoincrement());
    assertTrue(config.getTableDescription(catalog, "test", "TABLE2").getColumn("ID").isPK());

    assertFalse(config.getTableDescription(catalog, "test", "TABLE3").getColumn("ID1").isAutoincrement());
    assertTrue(config.getTableDescription(catalog, "test", "TABLE3").getColumn("ID1").isPK());
    assertFalse(config.getTableDescription(catalog, "test", "TABLE3").getColumn("ID2").isAutoincrement());
    assertTrue(config.getTableDescription(catalog, "test", "TABLE3").getColumn("ID2").isPK());

    assertEquals(config.getTableDescription(catalog, "test", "TABLE1").getPKColumns().size(), 1);
    assertEquals(config.getTableDescription(catalog, "test", "TABLE2").getPKColumns().size(), 1);
    assertEquals(config.getTableDescription(catalog, "test", "TABLE3").getPKColumns().size(), 2);
  }

  @Test(dataProviderClass = TestAbstractDB.class, dataProvider = "databases", dependsOnMethods = { "readStructureFromDatabase" })
  public void testPrimaryColumnReader(TestHelperPrepareDB.DBDriver driverClass, String url, String catalog) {
    DbConfig config = TestHelperPrepareDB.createConfig(driverClass, url);
    config.getReader().generateStructure();

    TableDesc tableDesc = config.getTableDescription(catalog, "test", "TABLE1");
    tableDesc.reloadRowsAction();
    assertEquals(tableDesc.getRows().size(), 3, "In the database must be 3 rows.");
    int i = driverClass == TestHelperPrepareDB.DBDriver.HSQL ? 0 : 1;
    for (RowDesc row : tableDesc.getRows()) {
      assertEquals(row.getColumnValue(tableDesc.getColumn("ID")), i);
      i++;
    }
  }
}
