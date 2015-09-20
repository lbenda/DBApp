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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 19.9.15.
 * Test of function inside the test table */
public class TestTableDesc {

  @DataProvider(name = "databases")
  public Object[][] createData1() {
    return TestHelperPrepareDB.databasesDataProvider();
  }

  @BeforeSuite
  private void setUp() {
    for (Tuple3<TestHelperPrepareDB.DBDriver, String, String> tuple3 : TestHelperPrepareDB.databases()) {
      TestHelperPrepareDB.prepareSmallDb(tuple3.get1(), tuple3.get2());
    }
  }

  @Test(dataProvider = "databases")
  private void testDirtyState(TestHelperPrepareDB.DBDriver driverClass, String url, String catalog) {
    DbConfig config = TestHelperPrepareDB.createConfig(driverClass, url);
    config.getReader().generateStructure();
    TableDesc tableDesc = config.getTableDescription(catalog, "test", "TABLE1");

    tableDesc.reloadRowsAction();
    Assert.assertFalse(tableDesc.dirtyStateProperty().getValue());
    RowDesc row = tableDesc.addNewRowAction();
    Assert.assertTrue(tableDesc.dirtyStateProperty().getValue());
    tableDesc.getRows().remove(row);
    Assert.assertFalse(tableDesc.dirtyStateProperty().getValue());

    tableDesc.addNewRowAction();
    tableDesc.reloadRowsAction();
    Assert.assertFalse(tableDesc.dirtyStateProperty().getValue());

    ColumnDesc columnDesc = tableDesc.getColumn("COL");
    row = tableDesc.getRows().get(0);
    //noinspection unchecked,ConstantConditions
    String puvodniHodnota = ((SimpleStringProperty) row.observableValueForColumn(columnDesc)).getValue();
    //noinspection unchecked,ConstantConditions
    ((SimpleStringProperty) row.observableValueForColumn(columnDesc)).setValue("Jina hodnota");
    Assert.assertTrue(tableDesc.dirtyStateProperty().getValue());
    ((SimpleStringProperty) row.observableValueForColumn(columnDesc)).setValue(puvodniHodnota);
    Assert.assertEquals(row.getState(), RowDesc.RowDescState.LOADED);
    Assert.assertFalse(tableDesc.dirtyStateProperty().getValue());
  }
}
