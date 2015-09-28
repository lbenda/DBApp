/*
 * Copyright 2015 Lukas Benda <lbenda at lbenda.cz>.
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
package cz.lbenda.dataman.db.dialect;

import java.util.Arrays;

/** @author Lukas Benda <lbenda at lbenda.cz> */
public class SQLDialectsHelper {

  /** Default Dialect */
  private static final SQLDialect DEFAULT = new H2Dialect();

  public static SQLDialect dialectForDriver(String driver) {
    if (SQLDialect.DIALECTS.isEmpty()) {
      SQLDialect.DIALECTS.addAll(Arrays.asList(new H2Dialect(), new HSQLDBDialect()));
    }
    for (SQLDialect dialect : SQLDialect.DIALECTS) {
      if (dialect.isForDriver(driver)) {
        return dialect;
      }
    }
    return DEFAULT;
  }
}
