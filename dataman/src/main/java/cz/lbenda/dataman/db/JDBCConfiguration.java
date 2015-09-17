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

import cz.lbenda.common.Obfuscate;
import cz.lbenda.dataman.db.dialect.SQLDialect;
import cz.lbenda.dataman.db.dialect.SQLDialectsHelper;

import cz.lbenda.dataman.schema.dataman.JdbcType;
import cz.lbenda.dataman.schema.dataman.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Instance of this object hold information about connection to JDBC, and cane generate some default connection URL's
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public class JDBCConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(JDBCConfiguration.class);

  private String username = ""; public void setUsername(final String username) { this.username = username; } public String getUsername() { return this.username; }
  private String password = ""; public void setPassword(final String password) { this.password = password; } public String getPassword() { return this.password; }
  private String driverClass = ""; public void setDriverClass(final String driverClass) { this.driverClass = driverClass; } public String getDriverClass() { return this.driverClass; }
  private String url = ""; public void setUrl(final String url) { this.url = url; } public String getUrl() { return this.url; }

  public void setUrlMySQL(final String serverName, final Integer portNumber) {
    if (portNumber != null) {
      this.url = String.format("jdbc:mysql://%s:%s/", serverName, portNumber);
    } else {
      this.url = String.format("jdbc:mysql://%s/", serverName);
    }
  }

  public void setUrlDerbyStandAlone(final String fileName) {
    this.url = String.format("jdbc:derby:%s;create=true", fileName);
  }

  public final void load(JdbcType jdbc) {
    setPassword(jdbc.getPassword() == null ? "" : Obfuscate.deobfuscate(jdbc.getPassword()));
    setUsername(jdbc.getUser() == null ? "" : jdbc.getUser());
    setUrl(jdbc.getUrl() == null ? "" : jdbc.getUrl());
    setDriverClass(jdbc.getDriverClass() == null ? "" : jdbc.getDriverClass());
  }

  public final JdbcType storeToJdbcType() {
    ObjectFactory of = new ObjectFactory();
    JdbcType jdbc = of.createJdbcType();
    jdbc.setDriverClass(driverClass);
    jdbc.setUser(getUsername());
    jdbc.setPassword(Obfuscate.obfuscate(getPassword()));
    jdbc.setUrl(getUrl());
    return jdbc;
  }

  public SQLDialect getDialect() {
    return SQLDialectsHelper.dialectForDriver(driverClass);
  }
}
