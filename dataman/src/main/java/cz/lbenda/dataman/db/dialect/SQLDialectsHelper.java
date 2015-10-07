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

import cz.lbenda.common.*;

import java.util.List;
import java.util.stream.Collectors;

/** @author Lukas Benda <lbenda at lbenda.cz> */
public class SQLDialectsHelper {

  /** Default Dialect */
  private final SQLDialect DEFAULT = new H2Dialect();

  private static SQLDialectsHelper instance;

  public static SQLDialectsHelper getInstance() {
    if (instance == null) { instance = new SQLDialectsHelper(); }
    return instance;
  }

  public SQLDialect dialectForDriver(String driver) {
    List<SQLDialect> find = SQLDialect.DIALECTS.stream().filter(dialect -> dialect.isForDriver(driver))
        .collect(Collectors.toList());
    if (find.isEmpty()) { return DEFAULT; }
    return find.get(0);
  }

  public SQLDialectsHelper() {
    List<String> classes = ClassLoaderHelper.classInPackage("cz.lbenda.dataman.db", getClass().getClassLoader());
    classes.forEach(className -> {
      try {
        Class clazz = getClass().getClassLoader().loadClass(className);
        //noinspection unchecked
        if (SQLDialect.class.isAssignableFrom(clazz)) {
          SQLDialect.DIALECTS.add((SQLDialect) clazz.newInstance());
        }
      } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
        /* The class exist. I can ignore this. */
      }
    });
  }
}
