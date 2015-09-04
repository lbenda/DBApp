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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.lbenda.schema.dbapp.dataman.*;
import cz.lbenda.schema.dbapp.exconf.*;
import cz.lbenda.schema.dbapp.exconf.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.*;

/** Object of this class define configuration of one session
 */
public class SessionConfiguration {

  private static final PropertyChangeSupport pcs = new PropertyChangeSupport(SessionConfiguration.class);

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
    try {
      JAXBContext jc = JAXBContext.newInstance(cz.lbenda.schema.dbapp.dataman.ObjectFactory.class);
      Unmarshaller u = jc.createUnmarshaller();
      JAXBElement o = (JAXBElement) u.unmarshal(new StringReader(document));
      if (o.getValue() instanceof DatamanType) {
        DatamanType dc = (DatamanType) o.getValue();
        if (dc.getSessions() == null || dc.getSessions().getSession().isEmpty()) {
          LOG.info("No configuration for loading");
        } else {
          configurations.clear();
          for (SessionType session : dc.getSessions().getSession()) {
            SessionConfiguration sc = new SessionConfiguration();
            configurations.add(sc);
            sc.fromSessionType(session);
          }
        }
      } else {
        LOG.error("The string didn't contains expected configuration: " + o.getClass().getName());
      }
    } catch (JAXBException e) {
      LOG.error("Problem with reading extended configuration: " + e.toString(), e);
    }
  }

  public static String storeToString() {
    cz.lbenda.schema.dbapp.dataman.ObjectFactory of = new cz.lbenda.schema.dbapp.dataman.ObjectFactory();
    DatamanType config = of.createDatamanType();
    config.setSessions(of.createSessionsType());

    for (SessionConfiguration sc : getConfigurations()) {
      config.getSessions().getSession().add(sc.storeToSessionType());
    }

    try {
      JAXBContext jc = JAXBContext.newInstance(cz.lbenda.schema.dbapp.dataman.ObjectFactory.class);
      Marshaller m = jc.createMarshaller();
      StringWriter sw = new StringWriter();
      m.marshal(of.createDataman(config), sw);
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      return sw.toString();
    } catch (JAXBException e) {
      LOG.error("Problem with write configuration: " + e.toString(), e);
      throw new RuntimeException("Problem with write configuration: " + e.toString(), e);
    }
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
  private ExtendedConfigTypeType extendedConfigType = ExtendedConfigTypeType.NONE; public final ExtendedConfigTypeType getExtendedConfigType() { return extendedConfigType; }
  /** Path to configuration where is the found extended */
  private String extendedConfigurationPath; public final String getExtendedConfigurationPath() { return this.extendedConfigurationPath; }
  /** Showed schemas */
  private final Map<String, List<String>> shownSchemas = new HashMap<>();
  /** Extended configuration of session */
  private ExConfType exConf;
  /** Instance of DB reader for this session */
  private DbStructureReader reader; public final DbStructureReader getReader() { return reader; }
  /** Timeout when unused connection will be closed */
  private int connectionTimeout; public int getConnectionTimeout() { return connectionTimeout; } public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

  /** Table description map */
  private final Map<String, Map<String, Map<String, TableDescription>>> tableDescriptionsMap
      = new HashMap<>();

  public final void setExtendedConfigType(ExtendedConfigTypeType extendedConfigType) {
    this.extendedConfigType = extendedConfigType;
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
    for (TableDescription.TableType r : set) {
      if (r == null) {
        set.remove(null);
        set.add(TableDescription.TableType.NULL);
      }
    }
    List<TableDescription.TableType> result = new ArrayList<>(set);
    Collections.sort(result);
    return result;
  }

  public final SessionType storeToSessionType() {
    cz.lbenda.schema.dbapp.dataman.ObjectFactory of = new cz.lbenda.schema.dbapp.dataman.ObjectFactory();
    SessionType result = of.createSessionType();
    result.setId(getId());
    if (this.connectionTimeout > 0) { result.setConnectionTimeout(this.connectionTimeout); }
    if (jdbcConfiguration != null) {
      result.setJdbc(jdbcConfiguration.storeToJdbcType());
    }

    result.setExtendedConfig(of.createExtendedConfigType());
    result.getExtendedConfig().setType(this.getExtendedConfigType());
    result.getExtendedConfig().setValue(this.getExtendedConfigurationPath());
    result.setLibraries(of.createLibrariesType());
    result.getLibraries().getLibrary().addAll(getLibrariesPaths());
    return result;
  }

  private void loadJdbcConfiguration(JdbcType jdbcType) {
    LOG.trace("load jdbc configuration");
    jdbcConfiguration = new JDBCConfiguration();
    jdbcConfiguration.load(jdbcType);
  }

  private void fromSessionType(final SessionType session) {
    setId(session.getId());
    if (session.getConnectionTimeout() != null) {
      this.connectionTimeout = Integer.valueOf(session.getConnectionTimeout());
    } else { connectionTimeout = -1; }
    loadJdbcConfiguration(session.getJdbc());
    ExtendedConfigType ed = session.getExtendedConfig();
    if (ed != null) {
      setExtendedConfigType(ed.getType());
      setExtendedConfigurationPath(ed.getValue());
    }
    this.librariesPaths.clear();
    if (session.getLibraries() != null) {
      for (String lib : session.getLibraries().getLibrary()) {
        this.librariesPaths.add(lib);
      }
    }
    loadExtendedConfiguration();
  }

  private void loadExtendedConfiguration() {
    switch (getExtendedConfigType()) {
      case FILE: loadExtendedConfigurationFromFile(); break;
      case DATABASE: // TODO implement
    }
  }

  private void tableOfKeysSQLFromElement(final TableOfKeySQLsType tableOfKeySQLs) {
    LOG.trace("load table of keys sql");
    if (tableOfKeySQLs == null || tableOfKeySQLs.getTableOfKeySQL().isEmpty()) {
      LOG.debug("No table of keys in configuration");
      return;
    }
    tableOfKeysSQL.clear();
    for (TableOfKeySQLType tableOfKey : tableOfKeySQLs.getTableOfKeySQL()) {
      this.tableOfKeysSQL.put(tableOfKey.getId(), tableOfKey.getValue());
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

  private void readTableConf(final ExConfType exConf) {
    TableDescriptionExtension.XMLReaderWriterHelper.loadExtensions(this, exConf);
  }

  /** Load schemas which will be showed */
  private void loadSchemas(final SchemasType schemas) {
    shownSchemas.clear();
    if (schemas == null || schemas.getSchema().isEmpty()) {
      LOG.debug("No schemas to configure");
      return;
    }
    for (SchemaType schema : schemas.getSchema()) {
      String catalog = schema.getCatalog();
      String sche = schema.getSchema();
      List<String> list = shownSchemas.get(catalog);
      if (list == null) {
        list = new ArrayList<>();
        shownSchemas.put(catalog, list);
      }
      list.add(sche);
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
    try {
      JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);
      Unmarshaller u = jc.createUnmarshaller();
      JAXBElement o = (JAXBElement) u.unmarshal(new File(extendedConfigurationPath));
      if (o.getValue() instanceof ExConfType) {
        this.exConf = (ExConfType) o.getValue();
        loadSchemas(exConf.getSchemas());
        tableOfKeysSQLFromElement(exConf.getTableOfKeySQLs());
        readTableConf(exConf);
      } else {
        LOG.error("The file didn't contains expected configuration: " + o.getClass().getName());
      }
    } catch (JAXBException e) {
      LOG.error("Problem with reading extended configuration: " + e.toString(), e);
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
