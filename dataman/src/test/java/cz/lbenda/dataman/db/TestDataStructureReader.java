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
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Map;


/** Created by Lukas Benda <lbenda @ lbenda.cz> on 19.9.15.
 * Testing implementatin of data structure reader
 */
@SuppressWarnings("ConstantConditions")
public class TestDataStructureReader {

  @DataProvider(name = "databases")
  public Object[][] createData1() {
    return new Object[][] {
        {TestHelperPrepareDB.DBDriver.HSQL, "jdbc:hsqldb:mem:smallTest", "PUBLIC"},
        {TestHelperPrepareDB.DBDriver.H2, "jdbc:h2:mem:smallTest;DB_CLOSE_DELAY=-1", "SMALLTEST" },
        {TestHelperPrepareDB.DBDriver.DERBY, "jdbc:derby:memory:smallTest;create=true", "" }
        /* {TestHelperPrepareDB.DBDriver.SQLITE, "jdbc:sqlite::memory:smallTest", "" } */ // FIXME the sqlite not avoid memory
    };
  }

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
    TestHelperPrepareDB.prepareSmallDb(driverClass, url);

    DbConfig config = new DbConfig();
    config.getJdbcConfiguration().setDriverClass(driverClass.getDriver());
    config.getJdbcConfiguration().setUrl(url);
    config.getJdbcConfiguration().setUsername(TestHelperPrepareDB.USERNAME);
    config.getJdbcConfiguration().setPassword(TestHelperPrepareDB.PASSWORD);
    DbStructureReader reader = new DbStructureReader(config);
    reader.generateStructure();
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
}
