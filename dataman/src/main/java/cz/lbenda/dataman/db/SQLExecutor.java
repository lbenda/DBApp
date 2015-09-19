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

import cz.lbenda.common.Tuple2;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 7.9.15.
 * This interface implements object which can execute SQL and return values from it */
public interface SQLExecutor {

  /** Execute operation on given prepared statement
   * The method beforeOpenInit prepared statement and close it.
   * @param sql SQL which is used to create prepared statement
   * @param consumer Consumer which get prepared statement and can work with it or SQL exception if is problem with
   *                 execute SQL command */
  void onPreparedStatement(String sql, Consumer<Tuple2<PreparedStatement, SQLException>> consumer);
}
