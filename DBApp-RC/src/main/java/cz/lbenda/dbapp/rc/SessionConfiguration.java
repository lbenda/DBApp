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
package cz.lbenda.dbapp.rc;

import cz.lbenda.dbapp.rc.db.JDBCConfiguration;
import cz.lbenda.dbapp.rc.db.SelectBoxTDExtension;
import cz.lbenda.dbapp.rc.db.TableDescription;
import cz.lbenda.dbapp.rc.db.TableDescriptionExtension;
import cz.lbenda.dbapp.rc.frm.config.DBConfigurationOptionsPanelController;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Object of this class define configuration of one session
 */
public class SessionConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(SessionConfiguration.class);

  /** All configuration over all system */
  private static final List<SessionConfiguration> configurations = new ArrayList<>();
  /** Flag which inform the configuration wasn't load yet */
  private static boolean configurationNotReadedYet = true;

  /** This method reload configuration which is stored in ntbeans pref in module DBConfiguraitonPanl */
  public static void reloadConfiguration() {
    configurations.clear();
    configurationNotReadedYet = false;
    String config = DBConfigurationOptionsPanelController.getConfigurationStr();
    if (config != null) { loadFromString(config); }
  }

  /** This method save configuration which is stored in netbeans pref in module DBConfigurationPanel */
  public static void saveConfiguration() {
    DBConfigurationOptionsPanelController.saveConfigurationStr(storeToString());
  }

  public static List<SessionConfiguration> getConfigurations() {
    if (configurationNotReadedYet) { reloadConfiguration(); }
    return configurations;
  }

  public static List<String> getConfigurationIDs() {
    ArrayList<String> result = new ArrayList<>();
    for (SessionConfiguration sc : getConfigurations()) {
      result.add(sc.getId());
    }
    return result;
  }

  public static SessionConfiguration getConfiguration(String configName) {
    if (configName == null) { throw new AssertionError("The parameter confiName can't be null."); }
    for (SessionConfiguration sc : getConfigurations()) {
      if (configName.equals(sc.getId())) { return sc; }
    }
    return null;
  }

  /** Method create new unset configuration with default values. But the new created configuration
   * isn't add to global list of all configurations
   */
  public static SessionConfiguration newConfiguration() {
    SessionConfiguration result = new SessionConfiguration();
    result.setId("<new id>");
    result.setJdbcConfiguration(new JDBCConfiguration());
    return result;
  }

  /** This method register new configuration to global list of configurations */
  public static void registerNewConfiguration(SessionConfiguration sc) {
    if (!configurations.contains(sc)) {
      configurations.add(sc);
    }
  }

  /** Remove configuration of given name from global list of configurations */
  public static void removeConfiguration(String name) {
    SessionConfiguration sc = getConfiguration(name);
    if (sc != null) { configurations.remove(sc); }
  }

  public static void loadFromString(final String document) {
    SAXBuilder builder = new SAXBuilder();
    try {
      loadFromDocument(builder.build(new StringReader(document)));
    } catch (JDOMException e) {
      LOG.error("The string isn't parsable: " + document, e);
      throw new RuntimeException("The string isn't parsable: " + document, e);
    } catch (IOException e) {
      LOG.error("The string can't be opend as StringReader: " + document, e);
      throw new RuntimeException("The string cant be opend as StringReader: " + document, e);
    }
  }

  @SuppressWarnings("unchecked")
  public static void loadFromDocument(final Document document) {
    configurations.clear();
    Element root = document.getRootElement();
    Element sessions = root.getChild("sessions");
    for (Element session : (List<Element>) sessions.getChildren("session")) {
      LOG.trace("loadFromDocument - session");
      SessionConfiguration sc = new SessionConfiguration();
      configurations.add(sc);
      sc.fromElement(session);
    }
  }

  public static String storeToString() {
    Document doc = new Document(storeAllToElement());
    XMLOutputter xmlOutput = new XMLOutputter();
    xmlOutput.setFormat(Format.getPrettyFormat());
    StringWriter sw = new StringWriter();
    try {
      xmlOutput.output(doc, sw);
      return sw.toString();
    } catch (IOException e) {
      LOG.error("There was problem with write XML output to StringWriter", e);
      throw new RuntimeException("There was problem with write XML output to StringWriter", e);
    }
  }

  public static Element storeAllToElement() {
    Element root = new Element("DBApp");
    Element sessions = new Element("sessions");
    root.addContent(sessions);
    for (SessionConfiguration sc : getConfigurations()) {
      sessions.addContent(sc.storeToElement());
    }
    return root;
  }

  /* NON-STATIC methods and variables */
  private String id; public final String getId() { return id; }
  private JDBCConfiguration jdbcConfiguration; public final JDBCConfiguration getJdbcConfiguration() { return jdbcConfiguration; } public final void setJdbcConfiguration(final JDBCConfiguration jdbcConfiguration) { this.jdbcConfiguration = jdbcConfiguration; }
  /** Map of table of keys. Every table of key have name as key and SQL which must have defined string. */
  private final Map<String, String> tableOfKeysSQL = new HashMap<>(); public final Map<String, String> getTableOfKeysSQL() { return tableOfKeysSQL; }
  /** Map which contains description rules for tables */
  private final List<TableDescription> extendedTD = new ArrayList<>(); public final List<TableDescription> getExtendedTD() { return extendedTD; }
  /** List with path to file with libraries for JDBC driver load */
  private final List<String> librariesPaths = new ArrayList<>(); public final List<String> getLibrariesPaths() { return librariesPaths; }

  public final Element storeToElement() {
    Element ses = new Element("session");
    ses.setAttribute("id", getId());
    if (jdbcConfiguration != null) { ses.addContent(jdbcConfiguration.storeToElement()); }
    if (!tableOfKeysSQL.isEmpty()) {
      Element tok = new Element("tableOfKeySQLs");
      ses.addContent(tok);
      for (Map.Entry<String, String> entry : tableOfKeysSQL.entrySet()) {
        tok.addContent(new Element("tableOfKeySQL").setAttribute("id", entry.getKey()).setText(entry.getValue()));
      }
    }
    if (!extendedTD.isEmpty()) {
      Element exts = new Element("tableDescriptionExtensions");
      ses.addContent(exts);
      for (TableDescription td : extendedTD) {
        Element ext = new Element("tableDescriptionExtension");
        exts.addContent(ext);
        ext.setAttribute("catalog", td.getCatalog()).setAttribute("schema", td.getSchema()).setAttribute("table", td.getName());
        for (TableDescriptionExtension tde : td.getExtensions()) {
          ext.addContent(tde.storeToElement());
        }
      }
    }
    if (!librariesPaths.isEmpty()) {
      Element libs = new Element("libraries");
      for (String path : getLibrariesPaths()) {
        Element lib = new Element("library");
        lib.setText(path);
        libs.addContent(lib);
      }
      ses.addContent(libs);
    }
    return ses;
  }

  private void jdbcConfigurationFromElement(Element element) {
    LOG.trace("load jdbc configuration");
    jdbcConfiguration = new JDBCConfiguration();
    jdbcConfiguration.loadFromElement(element);
  }

  private void fromElement(final Element element) {
    setId(element.getAttributeValue("id"));
    jdbcConfigurationFromElement(element.getChild("jdbc"));
    tableOfKeysSQLFromElement(element.getChild("tableOfKeySQLs"));
    tableDescriptionExtensionsFromElement(element.getChild("tableDescriptionExtensions"));
    Element e = element.getChild("libraries");
    this.librariesPaths.clear();
    if (e != null) {
      for (Element lib : (List<Element>) e.getChildren("library")) {
        this.librariesPaths.add(lib.getText());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void tableOfKeysSQLFromElement(final Element element) {
    LOG.trace("load table of keys sql");
    if (element == null) { return; }
    tableOfKeysSQL.clear();
    for (Element tableOfKey : (List<Element>) element.getChildren("tableOfKeySQL")) {
      this.tableOfKeysSQL.put(tableOfKey.getAttributeValue("id"), tableOfKey.getText());
    }
  }

  private TableDescription getOrCreateExtendedTableDescription(String catalog, String schema, String table) {
    for (TableDescription td : extendedTD) {
      if (td.getCatalog().equals(catalog) && td.getSchema().equals(schema) && td.getName().equals(table)) {
        return td;
      }
    }
    TableDescription td = new TableDescription(catalog, schema, null, table);
    this.extendedTD.add(td);
    return td;
  }

  @SuppressWarnings("unchecked")
  private void tableDescriptionExtensionsFromElement(final Element element) {
    LOG.trace("load table dsc. extension");
    if (element == null) { return; }
    for (Element tdExtension : (List<Element>) element.getChildren("tableDescriptionExtension")) {
      String catalog = tdExtension.getAttributeValue("catalog");
      String schema = tdExtension.getAttributeValue("schema");
      String table = tdExtension.getAttributeValue("table");
      TableDescription td = getOrCreateExtendedTableDescription(catalog, schema, table);

      for (Element select : (List<Element>) element.getChildren("selectBox")) {
        SelectBoxTDExtension sb = new SelectBoxTDExtension(td, select.getAttributeValue("column"),
            select.getAttributeValue("tableOfKeySQL"));
        sb.setColumnValue(select.getAttributeValue("column_value"));
        sb.setColumnChoice(select.getAttributeValue("column_choice"));
        sb.setColumnTooltip(select.getAttributeValue("column_tooltip"));
        for (Element reloadOn : (List<Element>) select.getChildren("reloadOnd")) {
          TableDescription td1 = getOrCreateExtendedTableDescription(reloadOn.getAttributeValue("catalog"),
              reloadOn.getAttributeValue("schema"), reloadOn.getAttributeValue("table"));
          td1.getReloadableExtension().add(sb);
        }
        td.getExtensions().add(sb);
      }
    }
  }

  public final void setId(final String id) {
    if (id == null) { throw new NullPointerException("The ID of configuration can't be null"); }
    if ("".equals(id.trim())) { throw new AssertionError("The identifier of configuration can't be blank or empty string."); }
    if (!id.equals(getId())) {
      if (SessionConfiguration.getConfigurationIDs().contains(id)) {
        throw new AssertionError(String.format("The ID '%s' of session is already used.", id));
      }
      this.id = id;
    }
  }

  @Override
  public String toString() { return id; }
}
