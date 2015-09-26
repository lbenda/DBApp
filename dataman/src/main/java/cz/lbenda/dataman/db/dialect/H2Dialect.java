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
package cz.lbenda.dataman.db.dialect;

/** @author Lukas Benda <lbenda at lbenda.cz> */
public class H2Dialect implements SQLDialect {

  static {
    SQLDialect.DIALECTS.add(new H2Dialect());
  }

  @Override
  public boolean isForDriver(String driver) {
    return driver != null && driver.startsWith("org.h2");
  }

  @Override
  public String columnGenerated() { return null; }
}
