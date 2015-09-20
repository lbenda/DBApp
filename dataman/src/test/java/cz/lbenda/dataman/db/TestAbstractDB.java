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
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;

import java.util.Map;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 20.9.15.
 * Abstract class which is used of tests which need configure database
 */
public abstract class TestAbstractDB {

  @DataProvider
  public static Object[][] databases() {
    return TestHelperPrepareDB.databasesDataProvider();
  }

  @BeforeGroups(groups = "database")
  public void setUp() {
    for (Tuple3<TestHelperPrepareDB.DBDriver, String, String> tuple3 : TestHelperPrepareDB.databases()) {
      TestHelperPrepareDB.prepareSmallDb(tuple3.get1(), tuple3.get2());
    }
  }

  @SuppressWarnings("unused")
  protected void writeStructure(DbConfig config) {
    for (Map.Entry<String, Map<String, Map<String, TableDesc>>> entry1 : config.getTableDescriptionsMap().entrySet()) {
      System.out.println(entry1.getKey());
      for (Map.Entry<String, Map<String, TableDesc>> entry2 : entry1.getValue().entrySet()) {
        System.out.println("  " + entry2.getKey());
        for (Map.Entry<String, TableDesc> entry3 : entry2.getValue().entrySet()) {
          System.out.println("    " + entry3.getKey());
          for (ColumnDesc columnDesc : entry3.getValue().getColumns()) {
            System.out.println("      " + columnDesc.getName());
          }
        }
      }
    }
  }
}
