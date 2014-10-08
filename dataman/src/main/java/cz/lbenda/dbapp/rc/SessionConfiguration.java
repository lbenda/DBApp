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

import cz.lbenda.dbapp.rc.db.DbStructureReader;
import cz.lbenda.dbapp.rc.db.JDBCConfiguration;
import cz.lbenda.dbapp.rc.db.TableDescription;
import cz.lbenda.dbapp.rc.db.TableDescriptionExtension;
import cz.lbenda.dbapp.rc.frm.config.DBConfigurationOptionsPanelController;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Object of this class define configuration of one session
 */
public class SessionConfiguration {

  private static final PropertyChangeSupport pcs = new PropertyChangeSupport(SessionConfiguration.class);

  public enum ExtendedConfigurationType {
    NONE, DATABASE, FILE, ;
  }

  private static final Logger LOG = LoggerFactory.getLogger(SessionConfiguration.class);

  /** All configuration over all system */
  private static final List<SessionConfiguration> configurations = new ArrayList<>();
  /** Flag which inform the configuration wasn't load yet */
  private static boolean configurationNotReadedYet = true;

  /** This method reload configuration which is stored in ntbeans pref in module DBConfiguraitonPanl */
  public static void reloadConfiguration() {
    SessionConfiguration.configurations.clear();
    SessionConfiguration.configurationNotReadedYet = false;
    String config = DBConfigurationOptionsPanelController.getConfigurationStr();
    if (config != null) { SessionConfiguration.loadFromString(config); }
    pcs.firePropertyChange("reloadConfiguration", null, null);
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
    if (!configurations.contains(sc)) { configurations.add(sc); }
    pcs.firePropertyChange("registerNewConfiguration", sc, sc);
  }

  /** Remove configuration of given name from global list of configurations */
  public static void removeConfiguration(String name) {
    SessionConfiguration sc = getConfiguration(name);
    if (sc != null) {
      configurations.remove(sc);
      pcs.firePropertyChange("remove", sc, null);
    }
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

  public static void loadFromDocument(final Document document) {
    configurations.clear();
    Element root = document.getRootElement();
    Element sessions = root.getChild("sessions");
    for (Element session : sessions.getChildren("session")) {
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
  private final List<TableDescription> tableDescriptions = new ArrayList<>(); public final List<TableDescription> getTableDescriptions() { return tableDescriptions; }
  /** List with path to file with libraries for JDBC driver load */
  private final List<String> librariesPaths = new ArrayList<>(); public final List<String> getLibrariesPaths() { return librariesPaths; }
  /** Type of extended configuration */
  private ExtendedConfigurationType extendedConfigurationType = ExtendedConfigurationType.NONE; public final ExtendedConfigurationType getExtendedConfigurationType() { return extendedConfigurationType; }
  /** Path to configuration where is the found extended */
  private String extendedConfigurationPath; public final String getExtendedConfigurationPath() { return this.extendedConfigurationPath; }
  /** Showed schemas */
  private final Map<String, List<String>> shownSchemas = new HashMap<>();
  /** Instance of DB reader for this session */
  private DbStructureReader reader; public final DbStructureReader getReader() { return reader; }
  /** Timeout when unused connection will be closed */
  private int connectionTimeout; public int getConnectionTimeout() { return connectionTimeout; } public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

  /** Table description map */
  private final Map<String, Map<String, Map<String, TableDescription>>> tableDescriptionsMap
      = new HashMap<>();

  public final void setExtendedConfigurationType(ExtendedConfigurationType extendedConfigurationType) {
    this.extendedConfigurationType = extendedConfigurationType;
  }

  public final void setExtendedConfigurationPath(String extendedConfigurationPath) {
    if (!AbstractHelper.nullEquals(extendedConfigurationPath, this.extendedConfigurationPath)) {
      this.extendedConfigurationPath = extendedConfigurationPath;
      loadExtendedConfiguration();
    }
  }

  /** Check if table will be show to user
   * @param td table description
   * @return true if table is shown
   */
  public final boolean isShowTable(final TableDescription td) {
    if (shownSchemas.isEmpty()) { return true; }
    if (!shownSchemas.containsKey(td.getCatalog())) { return false; }
    return shownSchemas.get(td.getCatalog()).contains(td.getSchema());
  }

  /** Inform if the catalog will be show.
   * @param catalog catalog which is show
   * @return catalog is show or not
   */
  public final boolean isShowCatalog(final String catalog) {
    if (catalog == null) { return false; }
    if (shownSchemas.isEmpty()) { return true; }
    return shownSchemas.containsKey(catalog) && shownSchemas.size() > 1;
  }

  /** Inform if the catalog will be show.
   * @param catalog catalog which is show
   * @param schema schema which is show
   * @return catalog is show or not
   */
  public final boolean isShowSchema(final String catalog, final String schema) {
    if (schema == null) { return true; }
    if (shownSchemas.isEmpty()) { return true; }
    if (!shownSchemas.containsKey(catalog)) { return false; }
    return shownSchemas.get(catalog).contains(schema) && shownSchemas.get(catalog).size() > 1;
  }

  /** List of shown table type (sorted)
   * @param catalog catalog which is show
   * @param schema schema which is show
   * @return catalog is show or not
   */
  public final List<TableDescription.TableType> shownTableType(final String catalog, final String schema) {
    Set<TableDescription.TableType> set = new HashSet<>(2);
    for (TableDescription td : this.tableDescriptionsMap.get(catalog).get(schema).values()) {
      if (isShowTable(td)) { set.add(td.getTableType()); }
    }
    List<TableDescription.TableType> result = new ArrayList<>(set);
    Collections.sort(result);
    return result;
  }

  public final Element storeToElement() {
    Element ses = new Element("session");
    ses.setAttribute("id", getId());
    if (this.connectionTimeout > 0) {
      ses.addContent(new Element("connectionTimeout").setText(Integer.toString(this.connectionTimeout)));
    }
    if (jdbcConfiguration != null) { ses.addContent(jdbcConfiguration.storeToElement()); }
    Element ed = new Element("extendedDescription");
    ed.setAttribute("type", this.getExtendedConfigurationType().name());
    ed.setText(this.getExtendedConfigurationPath());
    ses.addContent(ed);
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
    if (element.getChild("connectionTimeout") != null) {
      this.connectionTimeout = Integer.valueOf(element.getChildText("connectionTimeout"));
    } else { connectionTimeout = -1; }
    jdbcConfigurationFromElement(element.getChild("jdbc"));
    // tableOfKeysSQLFromElement(element.getChild("tableOfKeySQLs"));
    Element ed = element.getChild("extendedDescription");
    if (ed != null) {
      setExtendedConfigurationType(ExtendedConfigurationType.valueOf(ed.getAttributeValue("type")));
      setExtendedConfigurationPath(ed.getText());
    }
    Element e = element.getChild("libraries");
    this.librariesPaths.clear();
    if (e != null) {
      for (Element lib : e.getChildren("library")) {
        this.librariesPaths.add(lib.getText());
      }
    }
    loadExtendedConfiguration();
  }

  private void loadExtendedConfiguration() {
    switch (getExtendedConfigurationType()) {
      case FILE: loadExtendedConfigurationFromFile(); break;
      case DATABASE: // TODO implement
    }
  }

  private void tableOfKeysSQLFromElement(final Element element) {
    LOG.trace("load table of keys sql");
    if (element == null) { return; }
    tableOfKeysSQL.clear();
    for (Element tableOfKey : element.getChildren("tableOfKeySQL")) {
      this.tableOfKeysSQL.put(tableOfKey.getAttributeValue("id"), tableOfKey.getText());
    }
  }

  public final TableDescription getOrCreateTableDescription(String catalog, String schema, String table) {
    for (TableDescription td : getTableDescriptions()) {
      if (AbstractHelper.nullEquals(td.getCatalog(), catalog)
              && AbstractHelper.nullEquals(td.getSchema(), schema)
              && AbstractHelper.nullEquals(td.getName(), table)) {
        return td;
      }
    }
    TableDescription td = new TableDescription(catalog, schema, null, table);
    td.setSessionConfiguration(this);
    getTableDescriptions().add(td);
    Collections.sort(getTableDescriptions());

    Map<String, Map<String, TableDescription>> catgMap = tableDescriptionsMap.get(td.getCatalog());
    if (catgMap == null) {
      catgMap = new HashMap<>();
      tableDescriptionsMap.put(td.getCatalog(), catgMap);
    }
    Map<String, TableDescription> schMap = catgMap.get(td.getSchema());
    if (schMap == null) {
      schMap = new HashMap<>();
      catgMap.put(td.getSchema(), schMap);
    }
    schMap.put(td.getName(), td);

    return td;
  }

  private void tableDescriptionExtensionsFromElement(final Element element) {
    TableDescriptionExtension.XMLReaderWriterHelper.loadExtensions(this, element);
  }

  /** Load schemas which will be showed */
  private void loadSchemas(final Element element) {
    shownSchemas.clear();
    for (Element sch : element.getChildren("schema")) {
      String catalog = sch.getAttributeValue("catalog");
      String schema = sch.getAttributeValue("schema");
      List<String> list = shownSchemas.get(catalog);
      if (list == null) {
        list = new ArrayList<>();
        shownSchemas.put(catalog, list);
      }
      list.add(schema);
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

  private void loadExtendedConfigurationFromFile() {
    SAXBuilder builder = new SAXBuilder();
    try {
      File file = new File(extendedConfigurationPath);
      Document document = builder.build(new FileReader(file));
      Element root = document.getRootElement();
      loadSchemas(root.getChild("schemas"));
      tableOfKeysSQLFromElement(root.getChild("tableOfKeySQLs"));
      tableDescriptionExtensionsFromElement(root.getChild("tableDescriptionExtensions"));
    } catch (JDOMException e) {
      LOG.error("The file isn't parsable: " + extendedConfigurationPath, e);
      throw new RuntimeException("The file isn't parsable: " + extendedConfigurationPath, e);
    } catch (IOException e) {
      LOG.error("The file can't be opend as StringReader: " + extendedConfigurationPath, e);
      throw new RuntimeException("The file cant be opend as StringReader: " + extendedConfigurationPath, e);
    }
  }

  public final TableDescription getTableDescription(String catalog, String schema, String table) {
    return tableDescriptionsMap.get(catalog).get(schema).get(table);
  }

  public void reloadStructure() {
    this.tableDescriptionsMap.clear();
    this.tableDescriptions.clear();

    loadExtendedConfiguration();
    reader = new DbStructureReader();
    reader.setSessionConfiguration(this);
    reader.generateStructure();
  }

  /** Return all catalogs in table */
  public Set<String> getCatalogs() {
    return tableDescriptionsMap.keySet();
  }

  /** return all schemas of given catalog name */
  public Set<String> getSchemas(String catalog) {
    if (tableDescriptionsMap.get(catalog) == null) { return Collections.emptySet(); }
    return tableDescriptionsMap.get(catalog).keySet();
  }

  /** Return all table types in given catalog and schema */
  public Set<TableDescription.TableType> getTableTypes(String catalog, String schema) {
    Set<TableDescription.TableType> result = new HashSet<>();
    for (TableDescription td : tableDescriptionsMap.get(catalog).get(schema).values()) {
      result.add(td.getTableType());
    }
    return result;
  }

  /** Return all tables of table type */
  public List<TableDescription> getTableDescriptions(String catalog, String schema, TableDescription.TableType tableType) {
    List<TableDescription> result = new ArrayList<>();
    for (TableDescription td : tableDescriptionsMap.get(catalog).get(schema).values()) {
      if (AbstractHelper.nullEquals(tableType, td.getTableType())) {
        result.add(td);
      }
    }
    return result;
  }

  public static void addPropertyChangeListener(PropertyChangeListener listener) {
    pcs.addPropertyChangeListener(listener);
  }

  public static void removePropertyChangeListener(PropertyChangeListener listener) {
    pcs.removePropertyChangeListener(listener);
  }

  @Override
  public String toString() { return id; }
}
