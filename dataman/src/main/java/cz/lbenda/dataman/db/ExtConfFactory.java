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

import cz.lbenda.common.AbstractHelper;
import cz.lbenda.dataman.schema.dataman.ExtendedConfigType;
import cz.lbenda.dataman.schema.dataman.ExtendedConfigTypeType;
import cz.lbenda.dataman.schema.dataman.ObjectFactory;
import cz.lbenda.dataman.schema.exconf.*;
import cz.lbenda.rcp.ExceptionMessageFrmController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 30.9.15.
 * Factory which return extended configuration */
public class ExtConfFactory {

  private static final Logger LOG = LoggerFactory.getLogger(ExtConfFactory.class);

  /** Type of extended configuration */
  private ObjectProperty<ExtendedConfigTypeType> configType = new SimpleObjectProperty<>(ExtendedConfigTypeType.NONE);
  public final ExtendedConfigTypeType getConfigType() { return configType.getValue(); }
  public final void setConfigType(ExtendedConfigTypeType configType) { this.configType.setValue(configType); }
  public final ObjectProperty<ExtendedConfigTypeType> configTypeProperty() { return configType; }
  /** Map of table of keys. Every table of key have name as key and SQL which must have defined string. */
  private final Map<String, String> tableOfKeysSQL = new HashMap<>();
  public final Map<String, String> getTableOfKeysSQL() { return tableOfKeysSQL; }

  /** Path to configuration where is the found extended */
  private StringProperty path = new SimpleStringProperty(null);
  public final String getPath() { return this.path.getValue(); }
  public final void setPath(String path) {
    if (!AbstractHelper.nullEquals(path, this.path.getValue())) { this.path.setValue(path); }
  }
  public final StringProperty pathProperty() { return path; }

  private final DbConfig dbConfig;

  public ExtConfFactory(@Nonnull DbConfig dbConfig) {
    this.dbConfig = dbConfig;
  }

  public void setExtendedConfigType(ExtendedConfigType extendedConfigType) {
    setConfigType(extendedConfigType.getType());
    setPath(extendedConfigType.getValue());
  }

  public ExtendedConfigType getExtendedConfigType() {
    ObjectFactory of = new ObjectFactory();
    ExtendedConfigType result = of.createExtendedConfigType();
    result.setType(getConfigType());
    result.setValue(getPath());
    return result;
  }

  /** Flag which inform if extended configuration is configured */
  public boolean isExtConfig() {
    return StringUtils.isBlank(getPath());
  }

  /** Load extend configuration to given database configuration */
  public void load() {
    switch (getConfigType()) {
      case FILE:
        try (FileReader fileReader = new FileReader(new File(getPath()))) {
          loadExtendedConfiguration(fileReader);
        } catch (IOException e) {
          LOG.error("Problem with read extend config from file: " + getPath(), e);
          ExceptionMessageFrmController.showException("Problem with read extend config from file: " + getPath(), e);
        }
        break;
      case DATABASE:
        dbConfig.getConnectionProvider().onPreparedStatement("select usr, exConf from "
            + getPath() + " where (usr = ? or usr is null or usr = '')", tuple2 -> {
              PreparedStatement ps = tuple2.get1();
              String extendConfiguration = null;
              try {
                ps.setString(1, dbConfig.getConnectionProvider().getUser().getUsername());
                try (ResultSet rs = ps.executeQuery()) {
                  while (rs.next()) {
                    if (rs.getString(1) == null && extendConfiguration == null) { // The null user is used only if no specific user configuration is read
                      extendConfiguration = rs.getString(2);
                    } else if (rs.getString(1) != null) {
                      extendConfiguration = rs.getString(2);
                    }
                  }
                }
              } catch (SQLException e) {
                LOG.error("Problem with read extend config from table: " + getPath(), e);
                ExceptionMessageFrmController.showException("Problem with read extend config from table: " + getPath(), e);
              }
              if (!StringUtils.isBlank(extendConfiguration)) {
                loadExtendedConfiguration(new StringReader(extendConfiguration));
              } else {
                StringUtils.isBlank(null);
              }
            }
        );
    }
  }

  private void readTableConf(final ExConfType exConf) {
    TableDescriptionExtension.XMLReaderWriterHelper.loadExtensions(dbConfig, exConf);
  }

  public void loadExtendedConfiguration(Reader reader) {
    if (reader == null) {
      loadSchemas(null);
      tableOfKeysSQLFromElement(null);
      readTableConf(null);
    } else {
      try {
        JAXBContext jc = JAXBContext.newInstance(cz.lbenda.dataman.schema.exconf.ObjectFactory.class);
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

  /** Load schemas which will be showed */
  private void loadSchemas(final SchemasType schemas) {
    if (schemas == null || schemas.getSchema().isEmpty()) {
      LOG.debug("No schemas to configure");
      return;
    }
    dbConfig.getCatalogs().parallelStream().map(CatalogDesc::getSchemas)
        .forEach(list -> list.parallelStream().forEach(schema -> schema.setHidden(true)));
    schemas.getSchema().stream().forEach(schema -> dbConfig.getCatalog(schema.getCatalog())
        .getSchema(schema.getSchema()).setHidden(false));
  }

  private void tableOfKeysSQLFromElement(final TableOfKeySQLsType tableOfKeySQLs) {
    LOG.trace("load table of keys sql");
    if (tableOfKeySQLs == null || tableOfKeySQLs.getTableOfKeySQL().isEmpty()) {
      LOG.debug("No table of keys in configuration");
      return;
    }
    tableOfKeySQLs.getTableOfKeySQL().forEach(tableOfKey ->
      this.tableOfKeysSQL.put(tableOfKey.getId(), tableOfKey.getValue()));
  }
}
