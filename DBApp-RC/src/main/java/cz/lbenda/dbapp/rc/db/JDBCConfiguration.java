/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.rc.db;

import java.util.ArrayList;
import java.util.List;
import org.jdom.Element;

/** Instance of this object hold information about connection to JDBC, and cane generate some default connection URL's
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public class JDBCConfiguration {

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
    jdbc.addContent(new Element("driverClasss").setText(driverClass));
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
}
