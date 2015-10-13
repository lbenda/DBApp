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
import cz.lbenda.test.JavaFXInitializer;
import javafx.beans.property.SimpleStringProperty;
import org.junit.BeforeClass;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 19.9.15.
 * Test of function inside the test table */
public class TestTableDesc extends TestAbstractDB {

  @BeforeClass()
  public void setUp() throws InterruptedException {
    JavaFXInitializer.initialize();
    cz.lbenda.common.Constants.IS_IN_DEVELOP_MODE = true;
    for (Tuple3<TestHelperPrepareDB.DBDriver, String, String> tuple3 : TestHelperPrepareDB.databases()) {
      TestHelperPrepareDB.prepareSmallDb(tuple3.get1(), tuple3.get2());
    }
  }

  @Test(dataProviderClass = TestAbstractDB.class, dataProvider = "databases")
  public void testDirtyState(TestHelperPrepareDB.DBDriver driverClass, String url, String catalog) {
    DbConfig config = TestHelperPrepareDB.createConfig(driverClass, url);
    config.getReader().generateStructure();
    if (config.getCatalog(catalog) == null || config.getCatalog(catalog).getSchema("text") == null) { return; }
    TableDesc tableDesc = config.getCatalog(catalog).getSchema("test").getTable("TABLE1");

    tableDesc.reloadRowsAction();
    Assert.assertFalse(tableDesc.dirtyProperty().getValue());
    RowDesc row = tableDesc.addNewRowAction();
    Assert.assertTrue(tableDesc.dirtyProperty().getValue());
    tableDesc.getRows().remove(row);
    Assert.assertFalse(tableDesc.dirtyProperty().getValue());

    tableDesc.addNewRowAction();
    tableDesc.reloadRowsAction();
    Assert.assertTrue(
        tableDesc.getRows().stream().allMatch(row1 -> row1.getState() == RowDesc.RowDescState.LOADED));
    Assert.assertFalse(tableDesc.dirtyProperty().getValue());

    ColumnDesc columnDesc = tableDesc.getColumn("COL");
    row = tableDesc.getRows().get(0);
    //noinspection unchecked,ConstantConditions
    String puvodniHodnota = ((SimpleStringProperty) row.valueProperty(columnDesc)).getValue();
    //noinspection unchecked,ConstantConditions
    ((SimpleStringProperty) row.valueProperty(columnDesc)).setValue("Jina hodnota");
    Assert.assertTrue(tableDesc.dirtyProperty().getValue());
    ((SimpleStringProperty) row.valueProperty(columnDesc)).setValue(puvodniHodnota);

    Assert.assertEquals(row.getState(), RowDesc.RowDescState.CHANGED);
    Assert.assertTrue(tableDesc.dirtyProperty().getValue());
  }
}
