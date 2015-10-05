package cz.lbenda.common;

import org.testng.annotations.Test;

import java.sql.Driver;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 4.10.15. */
@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public class TestClassLoaderHelper {

  private static final String HSQL_LIBRARY = "/home/benzin/work/java/Dataman/dataman/src/test/resources/cz/lbenda/dataman/db/hsqldb.jar";
  private static final String SQL_DRIVER_CLASS = "org.hsqldb.jdbcDriver";

  @Test
  public void testGetClassFromLibs() throws Exception {
    Class<Driver> driver
        = ClassLoaderHelper.getClassFromLibs(SQL_DRIVER_CLASS, Arrays.asList(HSQL_LIBRARY), false);
    assertNotNull(driver);
    assertEquals(driver.getName(), SQL_DRIVER_CLASS);
  }

  @Test
  public void testInstancesOfClass() throws Exception {
    List<String> clazzs = ClassLoaderHelper.instancesOfClass(java.sql.Driver.class, Arrays.asList(HSQL_LIBRARY),
        false, false);
    assertNotNull(clazzs);
    assertEquals(clazzs.size(), 2);
  }

  @Test
  public void testClassInPackage() throws Exception {
    List<String> result = ClassLoaderHelper.classInPackage("cz.lbenda", getClass().getClassLoader());
    assertNotNull(result);
    assertTrue(result.size() > 50);
  }
}