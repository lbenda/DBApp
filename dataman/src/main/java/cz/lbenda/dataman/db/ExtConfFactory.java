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

import cz.lbenda.dataman.schema.exconf.*;
import cz.lbenda.rcp.ExceptionMessageFrmController;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.*;
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
  /** Map of table of keys. Every table of key have name as key and SQL which must have defined string. */
  private final Map<String, String> tableOfKeysSQL = new HashMap<>();
  /** Map of defined table keys */
  public final Map<String, String> getTableOfKeysSQL() { return tableOfKeysSQL; }
  /** Database configuration */
  private final DbConfig dbConfig;
  /** Extended configuration */
  private ExConfType exConf;
  /** Extended configuration */
  public void setExConf(ExConfType exConfType) {
    if (exConfType != null) { this.exConf = exConfType; }
  }
  /** Path to extended configuration */
  public String getSrc() { return exConf.getSrc(); }
  /** Path to extended configuration */
  public void setSrc(String src) { exConf.setSrc(src); }

  public ExtConfFactory(@Nonnull DbConfig dbConfig) {
    this.dbConfig = dbConfig;
    ObjectFactory of = new ObjectFactory();
    exConf = of.createExConfType();
  }

  public ExConfType create() {
    ObjectFactory of = new ObjectFactory();
    if (StringUtils.isBlank(getSrc())) {
      return null;
    } else {
      ExConfType result = of.createExConfType();
      result.setSrc(getSrc());
      return result;
    }
  }

  /** Load extend configuration to given database configuration */
  public void load() {
    if (exConf != null && StringUtils.isBlank(exConf.getSrc())) {
      loadExConfType(exConf);
    } else if (exConf != null) {
      if (exConf.getSrc().startsWith("db://")) {
        String path = exConf.getSrc().substring(5, exConf.getSrc().length());
        dbConfig.getConnectionProvider().onPreparedStatement(
            String.format("select usr, exConf from %s where (usr = ? or usr is null or usr = '')", path), tuple2 -> {
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
            LOG.error("Problem with read extend config from table: " + exConf.getSrc(), e);
            ExceptionMessageFrmController.showException("Problem with read extend config from table: "
                + exConf.getSrc(), e);
          }
          if (!StringUtils.isBlank(extendConfiguration)) {
            loadExConfType(new StringReader(extendConfiguration));
          } else {
            StringUtils.isBlank(null);
          }
        });
      } else {
        try {
          FileSystemManager fsManager = VFS.getManager();
          FileObject file = fsManager.resolveFile(exConf.getSrc());
          if (!file.exists()) {
            ExceptionMessageFrmController.showException("File not exist: "  + exConf.getSrc());
          } else if (file.getChildren() == null || file.getChildren().length == 0) {
            new Thread(() -> {
              try {
                FileContent content = file.getContent();
                loadExConfType(new InputStreamReader(content.getInputStream()));
                content.close();
              } catch (FileSystemException e) {
                LOG.error("Problem with read extend config from file: " + exConf.getSrc(), e);
                ExceptionMessageFrmController.showException("Problem with read extend config from file: "
                    + exConf.getSrc(), e);
              }
            }).start();
          } else {
            ExceptionMessageFrmController.showException("The file type isn't supported: "  + exConf.getSrc());
          }
        } catch (FileSystemException e) {
          LOG.error("Problem with read extend config from file: " + exConf.getSrc(), e);
          ExceptionMessageFrmController.showException("Problem with read extend config from file: "
              + exConf.getSrc(), e);
        }
      }
    }
  }

  private void readTableConf(final ExConfType exConf) {
    TableDescriptionExtension.XMLReaderWriterHelper.loadExtensions(dbConfig, exConf);
  }

  private void loadExConfType(ExConfType exConf) {
    if (exConf == null) {
      loadSchemas(null);
      tableOfKeysSQLFromElement(null);
      readTableConf(null);
    } else {
      loadSchemas(exConf.getSchemas());
      tableOfKeysSQLFromElement(exConf.getTableOfKeySQLs());
      readTableConf(exConf);
    }
  }

  private void loadExConfType(Reader reader) {
    if (reader == null) {
      loadExConfType((ExConfType) null);
    } else {
      try {
        JAXBContext jc = JAXBContext.newInstance(cz.lbenda.dataman.schema.exconf.ObjectFactory.class);
        Unmarshaller u = jc.createUnmarshaller();
        JAXBElement o = (JAXBElement) u.unmarshal(reader);
        if (o.getValue() instanceof ExConfType) {
          loadExConfType((ExConfType) o.getValue());
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
