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

import cz.lbenda.common.Tuple2;
import cz.lbenda.dataman.User;
import cz.lbenda.dataman.db.dialect.SQLDialect;
import cz.lbenda.rcp.action.SavableRegistry;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 27.9.15.
 * Class which work with connection */
public class ConnectionProvider implements SQLExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(ConnectionProvider.class);

  private DbConfig dbConfig;
  private DatamanDataSource dataSource = null;
  private User user; public @Nonnull User getUser() { return user; } public void setUser(User user) { this.user = user; }
  public final BooleanProperty connected = new SimpleBooleanProperty(false);
  /** Savable register for whole db config. */
  private final SavableRegistry savableRegistry = SavableRegistry.newInstance();
  public @Nonnull SavableRegistry getSavableRegistry() { return savableRegistry; }

  public ConnectionProvider(@Nonnull DbConfig dbConfig) {
    this.dbConfig = dbConfig;
  }

  /** Inform if the reader is prepared for read data - the session configuration exist */
  public final boolean isPrepared() { return dbConfig != null; }
  public final boolean isConnected() { return connected.get(); }
  public final ReadOnlyBooleanProperty connectedProperty() { return connected; }

  /** SQLDialect for this db configuration */
  public SQLDialect getDialect() { return dbConfig.getJdbcConfiguration().getDialect(); }

  private void createDataSource() throws IllegalStateException {
    if (!isPrepared()) {
      throw new IllegalStateException("The DBStructureReader isn't yet prepared for create dataSource. Please check isPrepared() properties");
    }
    if (dataSource != null) { dataSource = null; }
    dataSource = new DatamanDataSource(dbConfig);
  }

  /** Close all connections */
  public void close() {
    if (this.savableRegistry != null) {
      if (!this.savableRegistry.close()) { return; }
    }
    if (dataSource != null) {
      try {
        dataSource.closeAllConnections();
        this.connected.set(false);
      } catch (SQLException e) {
        LOG.error("The connection isn't close.", e);
      }
    } else {
      this.connected.set(false);
    }
  }

  public Connection getConnection() throws RuntimeException {
    if (dataSource == null) { createDataSource(); }
    try {
      Connection result = dataSource.getConnection();
      if (result == null) { throw new RuntimeException("The connection isn't created"); }
      this.connected.set(!result.isClosed());
      return result;
    } catch (SQLException e) {
      LOG.error("Filed to get connection", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onPreparedStatement(String sql, Consumer<Tuple2<PreparedStatement, SQLException>> consumer) {
    try (Connection connection = getConnection()) {
      try (PreparedStatement ps = connection.prepareCall(sql)) {
        consumer.accept(new Tuple2<>(ps, null));
      }
    } catch (SQLException e) {
      LOG.debug("The sql fall down", e);
      consumer.accept(new Tuple2<>(null, e));
    }
  }
}
