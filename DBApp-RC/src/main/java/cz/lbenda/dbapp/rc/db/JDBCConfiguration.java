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
package cz.lbenda.dbapp.rc.db;

import cz.lbenda.dbapp.rc.db.dialect.SQLDialect;
import cz.lbenda.dbapp.rc.db.dialect.SQLDialectsHelper;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Instance of this object hold information about connection to JDBC, and cane generate some default connection URL's
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public class JDBCConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(JDBCConfiguration.class);

  private final List<String> importedLibreries = new ArrayList<>(5); public final List<String> getImportedLibreries() { return importedLibreries; }
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

  public final void loadFromElement(Element element) {
    setPassword(subElementText(element, "password", ""));
    setUsername(subElementText(element, "user", "SA"));
    setUrl(subElementText(element, "url", ""));
    setDriverClass(subElementText(element, "driverClass", ""));
    Element libraries = element.getChild("libraries");
    if (libraries != null) {
      for (Element librarie : (List<Element>) libraries.getChildren("librarie")) {
        getImportedLibreries().add(librarie.getValue());
      }
    }
  }

  private String subElementText(Element element, String name, String def) {
    Element el = element.getChild(name);
    if (el == null) { return def; }
    return el.getText();
  }


  public final Element storeToElement() {
    Element jdbc = new Element("jdbc");
    jdbc.addContent(new Element("driverClass").setText(driverClass));
    jdbc.addContent(new Element("user").setText(getUsername()));
    jdbc.addContent(new Element("password").setText(getPassword()));
    jdbc.addContent(new Element("url").setText(getUrl()));
    Element libraries = new Element("libraries");
    jdbc.addContent(libraries);
    if (importedLibreries != null) {
      for (String library : importedLibreries) {
        libraries.addContent(new Element("librarie").setText(library));
      }
    }
    return jdbc;
  }

  public SQLDialect getDialect() {
    return SQLDialectsHelper.dialectForDriver(driverClass);
  }
}
