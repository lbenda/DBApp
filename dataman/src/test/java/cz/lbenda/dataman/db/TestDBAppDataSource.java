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
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 9/24/14.
 * Test if Data source can create connection */
@Test
public class TestDBAppDataSource {

  private static Logger LOG = LoggerFactory.getLogger(TestDBAppDataSource.class);

  @Test
  public final void testGetConnection() {
    DbConfig sc = new DbConfig();
    JDBCConfiguration jdbc = new JDBCConfiguration();
    sc.setJdbcConfiguration(jdbc);

    jdbc.setDriverClass("org.hsqldb.jdbcDriver");
    jdbc.setUsername("SA");
    jdbc.setUrl("jdbc:hsqldb:mem:aname");
    /// jdbc.setUrl("jdbc:hsqldb:hsql://localhost");
    sc.getLibrariesPaths().add("/opt/hsqldb/lib/hsqldb.jar");

    DBAppDataSource ds = new DBAppDataSource(sc);
    try {
      ds.getConnection();
    } catch (Exception e) {
      LOG.error("Problem with create connection", e);
      Assert.fail("Problem with create connection: " + e.toString());
    }
  }
}
