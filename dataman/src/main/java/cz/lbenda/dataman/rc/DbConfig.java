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
package cz.lbenda.dataman.rc;

import cz.lbenda.common.AbstractHelper;
import cz.lbenda.dataman.db.DBAppConnection;
import cz.lbenda.dataman.db.DbStructureReader;
import cz.lbenda.dataman.db.JDBCConfiguration;
import cz.lbenda.dataman.db.TableDescriptionExtension;
import cz.lbenda.dataman.db.TableDesc;
import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cz.lbenda.dataman.schema.dataman.*;
import cz.lbenda.dataman.schema.exconf.*;
import cz.lbenda.dataman.schema.exconf.ObjectFactory;
import cz.lbenda.rcp.ExceptionMessageFrmController;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.xml.bind.*;

/** Object of this class define configuration of one database */
public class DbConfig {

  private static final Logger LOG = LoggerFactory.getLogger(DbConfig.class);

  private String id; public final String getId() {
    if (id == null) { id = "<new id>"; }
    return id;
  }
  private JDBCConfiguration jdbcConfiguration; public final JDBCConfiguration getJdbcConfiguration() {
    if (jdbcConfiguration == null) { this.jdbcConfiguration = new JDBCConfiguration(); }
    return jdbcConfiguration;
  }
  public final void setJdbcConfiguration(final JDBCConfiguration jdbcConfiguration) { this.jdbcConfiguration = jdbcConfiguration; }
  /** Map of table of keys. Every table of key have name as key and SQL which must have defined string. */
  private final Map<String, String> tableOfKeysSQL = new HashMap<>(); public final Map<String, String> getTableOfKeysSQL() { return tableOfKeysSQL; }
  /** Map which contains description rules for tables */
  private final List<TableDesc> tableDescriptions = new ArrayList<>(); public final List<TableDesc> getTableDescriptions() { return tableDescriptions; }
  /** List with path to file with libraries for JDBC driver load */
  private final List<String> librariesPaths = new ArrayList<>(); public final List<String> getLibrariesPaths() { return librariesPaths; }
  /** Type of extended configuration */
  private ExtendedConfigTypeType extendedConfigType = ExtendedConfigTypeType.NONE; public final ExtendedConfigTypeType getExtendedConfigType() { return extendedConfigType; }
  /** Path to configuration where is the found extended */
  private String extendedConfigurationPath; public final String getExtendedConfigurationPath() { return this.extendedConfigurationPath; }
  /** Showed schemas */
  private final Map<String, List<String>> shownSchemas = new HashMap<>();
  /** Instance of DB reader for this session */
  private DbStructureReader reader; public final DbStructureReader getReader() { return reader; }
  /** Timeout when unused connection will be closed */
  private int connectionTimeout; public int getConnectionTimeout() { return connectionTimeout; } public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

  /** Table description map */
  private final Map<String, Map<String, Map<String, TableDesc>>> tableDescriptionsMap = new HashMap<>(); public  Map<String, Map<String, Map<String, TableDesc>>> getTableDescriptionsMap() { return tableDescriptionsMap; }

  public boolean isConnected() { return reader != null && reader.isConnected(); }

  public final void setExtendedConfigType(ExtendedConfigTypeType extendedConfigType) {
    this.extendedConfigType = extendedConfigType;
  }

  public final void setExtendedConfigurationPath(String extendedConfigurationPath) {
    if (!AbstractHelper.nullEquals(extendedConfigurationPath, this.extendedConfigurationPath)) {
      this.extendedConfigurationPath = extendedConfigurationPath;
      if (isConnected()) { loadExtendedConfiguration(); }
    }
  }

  /** Check if table will be show to user
   * @param td table description
   * @return true if table is shown
   */
  public final boolean isShowTable(final TableDesc td) {
    return shownSchemas.isEmpty() || shownSchemas.containsKey(td.getCatalog())
        && shownSchemas.get(td.getCatalog()).contains(td.getSchema());
  }

  /** Inform if the catalog will be show.
   * @param catalog catalog which is show
   * @return catalog is show or not
   */
  public final boolean isShowCatalog(final String catalog) {
    return getCatalogs().size() > 1 &&
        catalog != null && (shownSchemas.isEmpty() || shownSchemas.containsKey(catalog) && shownSchemas.size() > 1);
  }

  /** Inform if the catalog will be show.
   * @param catalog catalog which is show
   * @param schema schema which is show
   * @return catalog is show or not
   */
  public final boolean isShowSchema(final String catalog, final String schema) {
    return schema == null || shownSchemas.isEmpty() || shownSchemas.containsKey(catalog)
        && shownSchemas.get(catalog).contains(schema) && shownSchemas.get(catalog).size() > 1;
  }

  /** List of shown table type (sorted)
   * @param catalog catalog which is show
   * @param schema schema which is show
   * @return catalog is show or not
   */
  public final List<TableDesc.TableType> shownTableType(final String catalog, final String schema) {
    Set<TableDesc.TableType> set = new HashSet<>(2);
    set.addAll(this.tableDescriptionsMap.get(catalog).get(schema).values().stream().filter(this::isShowTable)
        .map(TableDesc::getTableType).collect(Collectors.toList()));
    set.forEach(r -> {
      if (r == null) {
        set.remove(null);
        set.add(TableDesc.TableType.NULL);
      }
    });
    List<TableDesc.TableType> result = new ArrayList<>(set);
    Collections.sort(result);
    return result;
  }

  public final SessionType storeToSessionType() {
    cz.lbenda.dataman.schema.dataman.ObjectFactory of = new cz.lbenda.dataman.schema.dataman.ObjectFactory();
    SessionType result = of.createSessionType();
    result.setId(getId());
    if (this.connectionTimeout > 0) { result.setConnectionTimeout(this.connectionTimeout); }
    if (jdbcConfiguration != null) {
      result.setJdbc(jdbcConfiguration.storeToJdbcType());
    }
    result.setLibraries(of.createLibrariesType());
    result.getLibraries().getLibrary().addAll(getLibrariesPaths());

    result.setExtendedConfig(of.createExtendedConfigType());
    result.getExtendedConfig().setType(this.getExtendedConfigType());
    result.getExtendedConfig().setValue(this.getExtendedConfigurationPath());
    return result;
  }

  private void loadJdbcConfiguration(JdbcType jdbcType) {
    LOG.trace("load jdbc configuration");
    jdbcConfiguration = new JDBCConfiguration();
    jdbcConfiguration.load(jdbcType);
  }

  public void fromSessionType(final SessionType session, boolean readId) {
    if (readId) { setId(session.getId()); }
    if (session.getConnectionTimeout() != null) {
      this.connectionTimeout = session.getConnectionTimeout();
    } else { connectionTimeout = -1; }
    loadJdbcConfiguration(session.getJdbc());
    this.librariesPaths.clear();
    if (session.getLibraries() != null) {
      session.getLibraries().getLibrary().forEach(this.librariesPaths::add);
    }

    ExtendedConfigType ed = session.getExtendedConfig();
    if (ed != null) {
      setExtendedConfigType(ed.getType());
      setExtendedConfigurationPath(ed.getValue());
    }
  }

  private void loadExtendedConfiguration() {
    if (StringUtils.isBlank(extendedConfigurationPath)) {
      loadExtendedConfiguration(null);
      LOG.debug("No extend configuration for load");
      return;
    }
    if (reader == null) {
      this.reloadStructure();
      return;
    }

    switch (getExtendedConfigType()) {
      case FILE:
        try (FileReader fileReader = new FileReader(new File(extendedConfigurationPath))) {
          loadExtendedConfiguration(fileReader);
        } catch (IOException e) {
          LOG.error("Problem with read extend config from file: " + extendedConfigurationPath, e);
          ExceptionMessageFrmController.showException("Problem with read extend config from file: " + extendedConfigurationPath, e);
        }
        break;
      case DATABASE:
        String extendConfiguration = null;
        try (Connection connection = reader.getConnection()) {
          try (PreparedStatement ps = connection.prepareCall("select usr, exConf from "
              + extendedConfigurationPath + " where (usr = ? or usr is null or usr = '')")) {
            ps.setString(1, reader.getUser().getUsername());
            try (ResultSet rs = ps.executeQuery()) {
              while (rs.next()) {
                if (rs.getString(1) == null && extendConfiguration == null) { // The null user is used only if no specific user configuraiton is readed
                  extendConfiguration = rs.getString(2);
                } else if (rs.getString(1) != null ) {
                  extendConfiguration = rs.getString(2);
                }
              }
            }
          }
        } catch (SQLException e) {
          LOG.error("Problem with read extend config from table: " + extendedConfigurationPath, e);
          ExceptionMessageFrmController.showException("Problem with read extend config from table: " + extendedConfigurationPath, e);
        }
        if (!StringUtils.isBlank(extendConfiguration)) {
          loadExtendedConfiguration(new StringReader(extendConfiguration));
        } else { StringUtils.isBlank(null); }
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

  public final TableDesc getOrCreateTableDescription(String catalog, String schema, String table) {
    for (TableDesc td : getTableDescriptions()) {
      if (AbstractHelper.nullEquals(td.getCatalog(), catalog)
              && AbstractHelper.nullEquals(td.getSchema(), schema)
              && AbstractHelper.nullEquals(td.getName(), table)) {
        return td;
      }
    }
    TableDesc td = new TableDesc(catalog, schema, null, table);
    td.setDbConfig(this);
    getTableDescriptions().add(td);
    Collections.sort(getTableDescriptions());

    Map<String, Map<String, TableDesc>> catgMap = tableDescriptionsMap.get(td.getCatalog());
    if (catgMap == null) {
      catgMap = new HashMap<>();
      tableDescriptionsMap.put(td.getCatalog(), catgMap);
    }
    Map<String, TableDesc> schMap = catgMap.get(td.getSchema());
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
    schemas.getSchema().forEach(this::loadSchema);
  }

  /** Load schema which will be show */
  private void loadSchema(final SchemaType schema) {
    String catalog = schema.getCatalog();
    String sche = schema.getSchema();
    List<String> list = shownSchemas.get(catalog);
    if (list == null) {
      list = new ArrayList<>();
      shownSchemas.put(catalog, list);
    }
    list.add(sche);
  }

  public final void setId(final String id) {
    if (id == null) { throw new NullPointerException("The ID of configuration can't be null"); }
    if ("".equals(id.trim())) { throw new AssertionError("The identifier of configuration can't be blank or empty string."); }
    if (!id.equals(getId())) {
      if (DbConfigFactory.getConfigurationIDs().contains(id)) {
        throw new AssertionError(String.format("The ID '%s' of session is already used.", id));
      }
      this.id = id;
    }
  }

  private void loadExtendedConfiguration(Reader reader) {
    if (reader == null) {
      loadSchemas(null);
      tableOfKeysSQLFromElement(null);
      readTableConf(null);
    } else {
      try {
        JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);
        Unmarshaller u = jc.createUnmarshaller();
        JAXBElement o = (JAXBElement) u.unmarshal(reader);
        if (o.getValue() instanceof ExConfType) {
          ExConfType exConf = (ExConfType) o.getValue();
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
  }

  public final TableDesc getTableDescription(String catalog, String schema, String table) {
    return tableDescriptionsMap.get(catalog).get(schema).get(table);
  }

  public void reloadStructure() {
    this.tableDescriptionsMap.clear();
    this.tableDescriptions.clear();

    reader = new DbStructureReader(this);
    loadExtendedConfiguration();
    reader.generateStructure();
  }

  /** Close connection to database */
  public void close() {
    try {
      if (this.reader != null && this.reader.getConnection() != null && !this.reader.getConnection().isClosed()) {
        ((DBAppConnection) this.reader.getConnection()).realyClose();
      }
    } catch (SQLException e) {
      LOG.warn("The connection can't be closed.");
    }
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
  public Set<TableDesc.TableType> getTableTypes(String catalog, String schema) {
    Set<TableDesc.TableType> result = new HashSet<>();
    tableDescriptionsMap.get(catalog).get(schema).values().forEach(td -> result.add(td.getTableType()));
    return result;
  }

  /** Return all tables of table type */
  public List<TableDesc> getTableDescriptions(String catalog, String schema, TableDesc.TableType tableType) {
    List<TableDesc> result = new ArrayList<>();
    tableDescriptionsMap.get(catalog).get(schema).values().forEach(td -> {
      if (AbstractHelper.nullEquals(tableType, td.getTableType())) {
        result.add(td);
      }});
    return result;
  }

  public void save(Writer writer) throws IOException {
    cz.lbenda.dataman.schema.dataman.ObjectFactory of = new cz.lbenda.dataman.schema.dataman.ObjectFactory();
    SessionType st = storeToSessionType();
    try {
      JAXBContext jc = JAXBContext.newInstance(cz.lbenda.dataman.schema.dataman.ObjectFactory.class);
      Marshaller m = jc.createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      m.marshal(of.createSession(st), writer);
    } catch (JAXBException e) {
      LOG.error("Problem with write configuration: " + e.toString(), e);
      throw new RuntimeException("Problem with write configuration: " + e.toString(), e);
    }
  }

  /** Save session conf into File */
  public void save(File file) {
    if (StringUtils.isBlank(FilenameUtils.getExtension(file.getAbsolutePath()))) { file = new File(file.getAbsoluteFile() + ".dtm"); } // Append .dtm extension to file which haven't any extension
    try (FileWriter fw = new FileWriter(file)) {
      save(fw);
    } catch (IOException e) {
      LOG.error("The file is unwritable: " + e.toString(), e);
      throw new RuntimeException("The file is unwritable: " + e.toString(), e);
    }
  }

  /** Load configuration from given resource which is defined by URL */
  public void load(URL resource) {
    try (InputStream is = resource.openStream()) {
      load(is);
    } catch (IOException e) {
      LOG.error("Problem with read configuration from XML: " + e.toString(), e);
      throw new RuntimeException("Problem with read configuration from XML: " + e.toString(), e);
    }
  }

  /** Load configuration from given resource from input stream */
  @SuppressWarnings("unchecked")
  public void load(InputStream inputStream) {
    load(new InputStreamReader(inputStream),  true);
  }

  /** Load configuration from given resource from input stream
   * @param reader reader from which is read configuration
   * @param readId flag which inform if newId will be reader from reader or is ignored */
  public void load(@Nonnull Reader reader, boolean readId) {
    try {
      JAXBContext jc = JAXBContext.newInstance(cz.lbenda.dataman.schema.dataman.ObjectFactory.class);
      Unmarshaller u = jc.createUnmarshaller();
      JAXBElement<SessionType> element = (JAXBElement<SessionType>) u.unmarshal(reader);
      if (element.getValue() instanceof SessionType) {
        fromSessionType(element.getValue(), readId);
      } else {
        LOG.error("The file doesn't contains single session config");
        throw new RuntimeException("The file doesn't contains single session config");
      }
    } catch (JAXBException e) {
      LOG.error("Problem with read configuration from XML: " + e.toString(), e);
      throw new RuntimeException("Problem with read configuration from XML: " + e.toString(), e);
    }
  }

  /** Load session conf from File */
  public void load(File file) {
    try (FileReader fis = new FileReader(file)) {
      load(fis, true);
    } catch (IOException e) {
      LOG.error("File is unreadable: " + e.toString(), e);
      throw new RuntimeException("The file is unreadable: " + e.toString(), e);
    }
  }

  @Override
  public String toString() { return id; }

  /** Copy configuration of db config. It's not a clone, the connection and loaded data isn't copied.
   * @return copied object */
  public DbConfig copy() {
    try {
      StringWriter writer = new StringWriter();
      this.save(writer);
      DbConfig dbConfig = new DbConfig();
      StringReader reader = new StringReader(writer.toString());
      dbConfig.load(reader, false);
      return dbConfig;
    } catch (IOException e){
      LOG.error("Problem with create copied version of db config", e);
      throw new RuntimeException("Problem with create copied version of db config", e);
    }
  }
}
