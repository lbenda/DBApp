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

import java.io.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cz.lbenda.dataman.UserImpl;
import cz.lbenda.dataman.db.dialect.SQLDialect;
import cz.lbenda.dataman.rc.DbConfigFactory;
import cz.lbenda.dataman.schema.dataman.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
  /** List with path to file with libraries for JDBC driver load */
  private final List<String> librariesPaths = new ArrayList<>(); public final List<String> getLibrariesPaths() { return librariesPaths; }
  /** Showed schemas */
  private final ObservableList<CatalogDesc> catalogs = FXCollections.observableArrayList();
  public final ObservableList<CatalogDesc> getCatalogs() { return catalogs; }
  /** Return catalog by name */
  public final CatalogDesc getCatalog(@Nonnull String name) {
    List<CatalogDesc> cats = catalogs.stream().filter(catalogDesc -> name.equals(catalogDesc.getName())).collect(Collectors.toList());
    return cats.size() == 0 ? null : cats.get(0);
  }

  /** SQLDialect for this db configuration */
  public final SQLDialect getDialect() { return getJdbcConfiguration().getDialect(); }
  /** Connection provider */
  public ConnectionProvider connectionProvider = new ConnectionProvider(this);
  /** Connection provider */
  public @Nonnull ConnectionProvider getConnectionProvider() { return connectionProvider; }
  /** Object which can modify database row */
  private final DbRowManipulator dbRowManipulator = new DbRowManipulator(connectionProvider);
  /** Manipulator of DB */
  public final @Nonnull DbRowManipulator getDbRowManipulator() { return dbRowManipulator; }
  /** Instance of DB reader for this session */
  private DbStructureFactory reader = new DbStructureFactory(this);
  public final @Nonnull
  DbStructureFactory getReader() { return reader; }
  final void setReader(@Nonnull DbStructureFactory reader) { this.reader = reader; }
  /** Timeout when unused connection will be closed */
  private int connectionTimeout; public int getConnectionTimeout() { return connectionTimeout; } public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

  private ExtConfFactory extConfFactory = new ExtConfFactory(this);
  public ExtConfFactory getExtConfFactory() { return extConfFactory; }

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
    result.setExtendedConfig(this.extConfFactory.getExtendedConfigType());
    result.setStructure(DbStructureFactory.createXMLDatabaseStructure(getCatalogs()));
    return result;
  }

  private void loadJdbcConfiguration(JdbcType jdbcType) {
    jdbcConfiguration = new JDBCConfiguration();
    jdbcConfiguration.load(jdbcType);
    this.getConnectionProvider().setUser(new UserImpl(jdbcConfiguration.getUsername()));
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
    new Thread(() -> {
      List<CatalogDesc> cds = DbStructureFactory.loadDatabaseStructureFromXML(session.getStructure(), this);
      Platform.runLater(() -> catalogs.addAll(cds));
    }).start();
    extConfFactory.setExtendedConfigType(session.getExtendedConfig());
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

  public void reloadStructure() {
    reader.generateStructure();
    this.extConfFactory.load();
    DbConfigFactory.saveConfiguration();
  }

  /** Close connection to database */
  @SuppressWarnings("unused")
  public void close() {
    try {
      if (connectionProvider.getConnection() != null && !connectionProvider.getConnection().isClosed()) {
        ((DatamanConnection) connectionProvider.getConnection()).realyClose();
      }
    } catch (SQLException e) {
      LOG.warn("The connection can't be closed.");
    }
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
  @SuppressWarnings("unused")
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
      //noinspection unchecked
      JAXBElement<SessionType> element = (JAXBElement<SessionType>) u.unmarshal(reader);
      //noinspection ConstantConditions
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

  public static class Reload extends Task<Void> {
    private DbConfig dbConfig;
    public Reload(DbConfig dbConfig) {
      this.dbConfig = dbConfig;
    }
    @Override
    protected Void call() throws Exception {
      dbConfig.reloadStructure();
      return null;
    }
  }
}
