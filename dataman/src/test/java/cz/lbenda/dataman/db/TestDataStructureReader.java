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

import cz.lbenda.common.Tuple3;
import cz.lbenda.dataman.rc.DbConfig;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;


/** Created by Lukas Benda <lbenda @ lbenda.cz> on 19.9.15.
 * Testing implementation of data structure reader
 */
@SuppressWarnings("ConstantConditions")
public class TestDataStructureReader {

  @DataProvider(name = "databases")
  public Object[][] createData1() {
    return TestHelperPrepareDB.databasesDataProvider();
  }

  @BeforeClass
  private void setUp() {
    for (Tuple3<TestHelperPrepareDB.DBDriver, String, String> tuple3 : TestHelperPrepareDB.databases()) {
      TestHelperPrepareDB.prepareSmallDb(tuple3.get1(), tuple3.get2());
    }
  }

  @SuppressWarnings("unused")
  private void writeStructure(DbConfig config) {
    for (Map.Entry<String, Map<String, Map<String, TableDesc>>> entry1 : config.getTableDescriptionsMap().entrySet()) {
      System.out.println(entry1.getKey());
      for (Map.Entry<String, Map<String, TableDesc>> entry2 : entry1.getValue().entrySet()) {
        System.out.println("  " + entry2.getKey());
        for (Map.Entry<String, TableDesc> entry3 : entry2.getValue().entrySet()) {
          System.out.println("    " + entry3.getKey());
          Assert.assertTrue(entry3.getValue().getColumns().size() > 0, "Table haven't column");
          for (ColumnDesc columnDesc : entry3.getValue().getColumns()) {
            System.out.println("      " + columnDesc.getName());
          }
        }
      }
    }
  }

  @Test(dataProvider = "databases")
  public void readStructureFromDatabase(TestHelperPrepareDB.DBDriver driverClass, String url, String catalog) {
    DbConfig config = TestHelperPrepareDB.createConfig(driverClass, url);
    config.getReader().generateStructure();
    // writeStructure(config); // Could be uncommented for seeing read struct

    Assert.assertNotNull(config.getSchemas(catalog));
    Assert.assertTrue(config.getSchemas(catalog).contains("test"));
    Assert.assertNotNull(config.getTableDescription(catalog, "test", "TABLE1"));
    Assert.assertTrue(config.getTableDescription(catalog, "test", "TABLE1").getColumn("ID").isAutoincrement());
    Assert.assertTrue(config.getTableDescription(catalog, "test", "TABLE1").getColumn("ID").isPK());

    Assert.assertFalse(config.getTableDescription(catalog, "test", "TABLE2").getColumn("ID").isAutoincrement());
    Assert.assertTrue(config.getTableDescription(catalog, "test", "TABLE2").getColumn("ID").isPK());

    Assert.assertFalse(config.getTableDescription(catalog, "test", "TABLE3").getColumn("ID1").isAutoincrement());
    Assert.assertTrue(config.getTableDescription(catalog, "test", "TABLE3").getColumn("ID1").isPK());
    Assert.assertFalse(config.getTableDescription(catalog, "test", "TABLE3").getColumn("ID2").isAutoincrement());
    Assert.assertTrue(config.getTableDescription(catalog, "test", "TABLE3").getColumn("ID2").isPK());

    Assert.assertEquals(config.getTableDescription(catalog, "test", "TABLE1").getPKColumns().size(), 1);
    Assert.assertEquals(config.getTableDescription(catalog, "test", "TABLE2").getPKColumns().size(), 1);
    Assert.assertEquals(config.getTableDescription(catalog, "test", "TABLE3").getPKColumns().size(), 2);
  }

  @Test(dataProvider = "databases", dependsOnMethods = { "readStructureFromDatabase" })
  public void testPrimaryColumnReader(TestHelperPrepareDB.DBDriver driverClass, String url, String catalog) {
    DbConfig config = TestHelperPrepareDB.createConfig(driverClass, url);
    config.getReader().generateStructure();

    TableDesc tableDesc = config.getTableDescription(catalog, "test", "TABLE1");
    tableDesc.reloadRowsAction();
    Assert.assertEquals(tableDesc.getRows().size(), 3, "In the database must be 3 rows.");
    int i = driverClass == TestHelperPrepareDB.DBDriver.HSQL ? 0 : 1;
    for (RowDesc row : tableDesc.getRows()) {
      Assert.assertEquals(row.getColumnValue(tableDesc.getColumn("ID")), i);
      i++;
    }
  }
}
