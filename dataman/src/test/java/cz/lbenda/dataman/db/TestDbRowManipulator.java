package cz.lbenda.dataman.db;

import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.*;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 5.10.15. */
public class TestDbRowManipulator extends TestAbstractDB {

  /** Test saving changes when new row is inserted */
  @Test(dataProviderClass = TestAbstractDB.class, dataProvider = "databases", groups = "database")
  public void testSaveInsert(TestHelperPrepareDB.DBDriver driverClass, String url, String catalog) {
    DbConfig config = TestHelperPrepareDB.createConfig(driverClass, url);
    config.getReader().generateStructure();

    TableDesc td = config.getCatalog(catalog).getSchema("test").getTable("TABLE1");
    td.reloadRowsAction();
    int originalSize = td.getRows().size();

    RowDesc rd = td.addNewRowAction();
    rd.setColumnValue(td.getColumn("COL"), "coll");
    config.getDbRowManipulator().saveChanges(td);
    assertNotNull(rd.getColumnValue(td.getColumn("ID")));

    System.out.println("driverClass" + driverClass + ", " + config.getDialect());

    Object id = rd.getColumnValue(td.getColumn("ID"));
    int expectedId = originalSize + config.getDialect().incrementFrom();
    if (id instanceof Long) {
      //noinspection UnnecessaryBoxing
      assertEquals(id, Long.valueOf(expectedId));
    } else if (id instanceof BigDecimal) {
      System.out.print(id.getClass());
      assertEquals(id, BigDecimal.valueOf(expectedId));
    } else {
      //noinspection UnnecessaryBoxing
      assertEquals(id, Integer.valueOf(expectedId));
    }
  }
}