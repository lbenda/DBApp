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

import cz.lbenda.dataman.schema.exconf.ObjectFactory;
import org.testng.annotations.Test;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 30.9.15.
 * Test of db config */
public class TestDbConfig extends TestAbstractDB {

  @Test(dataProviderClass = TestAbstractDB.class, dataProvider = "databases", groups = "database")
  public void extendConfiguration(TestHelperPrepareDB.DBDriver driverClass, String url, String catalog) {
    DbConfig config = TestHelperPrepareDB.createConfig(driverClass, url);
    ObjectFactory objectFactory = new ObjectFactory();
    // ExtendedConfigType extendedConfigType = objectFactory.createEx

    config.getReader().generateStructure();
  }

}
