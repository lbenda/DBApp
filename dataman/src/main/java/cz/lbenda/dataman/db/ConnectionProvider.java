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

import cz.lbenda.dataman.User;
import cz.lbenda.dataman.UserImpl;
import cz.lbenda.dataman.db.dialect.SQLDialect;
import cz.lbenda.rcp.action.SavableRegistry;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.SQLException;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 27.9.15.
 * Class which work with connection */
public class ConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ConnectionProvider.class);

  private DbConfig dbConfig;
  private DBAppDataSource dataSource = null;
  private User user; public User getUser() { return user; }
  private boolean connected = false;
  /** Savable register for whole db config. */
  private SavableRegistry savableRegistry = SavableRegistry.newInstance();
  public @Nonnull SavableRegistry getSavableRegistry() { return savableRegistry; }

  public ConnectionProvider(@Nonnull DbConfig dbConfig) {
    this.dbConfig = dbConfig;
    if (dbConfig.getJdbcConfiguration() != null) {
      user = new UserImpl(dbConfig.getJdbcConfiguration().getUsername());
    }
  }

  /** Inform if the reader is prepared for read data - the session configuration exist */
  public final boolean isPrepared() { return dbConfig != null; }
  public final boolean isConnected() { return connected; }

  /** SQLDialect for this db configuration */
  public SQLDialect getDialect() { return dbConfig.getJdbcConfiguration().getDialect(); }

  private void createDataSource() throws IllegalStateException {
    if (!isPrepared()) {
      throw new IllegalStateException("The DBStructureReader isn't yet prepared for create dataSource. Please check isPrepared() properties");
    }
    if (dataSource != null) { dataSource = null; }
    dataSource = new DBAppDataSource(dbConfig);
  }

  /** Close all connections */
  public void close(@Nonnull Stage stage) {
    if (this.savableRegistry != null) {
      if (!this.savableRegistry.close(stage)) { return; }
      this.savableRegistry = null;
    }
    if (dataSource != null) {
      try {
        dataSource.closeAllConnections();
        this.connected = false;
      } catch (SQLException e) {
        LOG.error("The connection isn't close.", e);
      }
    } else {
      this.connected = false;
    }
  }

  public Connection getConnection() throws RuntimeException {
    if (dataSource == null) { createDataSource(); }
    try {
      Connection result = dataSource.getConnection();
      if (result == null) { throw new RuntimeException("The connection isn't created"); }
      this.connected = !result.isClosed();
      return result;
    } catch (SQLException e) {
      LOG.error("Filed to get connection", e);
      throw new RuntimeException(e);
    }
  }
}
