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
package cz.lbenda.dataman.db.frm;

import cz.lbenda.common.Tuple2;
import cz.lbenda.dataman.db.ColumnDesc;
import cz.lbenda.dataman.db.DbStructureFactory;
import cz.lbenda.dataman.db.TableDesc;
import cz.lbenda.dataman.db.dialect.ColumnType;
import cz.lbenda.rcp.localization.Message;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.10.15.
 * Frame controller for view which show detail information about table */
public class DbTableStructureFrmController implements Initializable {

  private static Logger LOG = LoggerFactory.getLogger(DbTableStructureFrmController.class);
  private static final String FXML_RESOURCE = "DbTableStructureFrm.fxml";

  @Message
  public static final String WINDOW_TITLE = "Structure";

  @FXML
  private Label lTableName;
  @FXML
  private Label lQualifiedName;
  @FXML
  private Label lTableType;
  @FXML
  private TextArea taComment;
  @FXML
  private TableView<ColumnDesc> tvColumns;
  @FXML
  private TableColumn<ColumnDesc, Boolean> tcColumnsPK;
  @FXML
  private TableColumn<ColumnDesc, String> tcColumnsName;
  @FXML
  private TableColumn<ColumnDesc, String> tcColumnsComment;
  @FXML
  private TableColumn<ColumnDesc, ColumnType> tcColumnsType;
  @FXML
  private TableColumn<ColumnDesc, Boolean> tcColumnsNull;
  @FXML
  private TableColumn<ColumnDesc, Integer> tcColumnsLength;
  @FXML
  private TableColumn<ColumnDesc, Integer> tcColumnsScale;
  @FXML
  private TableColumn<ColumnDesc, String> tcColumnsDefault;
  @FXML
  private TableColumn<ColumnDesc, Boolean> tcColumnsEditable;
  @FXML
  private TableView<DbStructureFactory.ForeignKey> tvForeignKeys;

  @FXML
  private TableColumn<DbStructureFactory.ForeignKey, String> tcForeignKeyInOut;
  @FXML
  private TableColumn<DbStructureFactory.ForeignKey, String> tcForeignKeyName;
  @FXML
  private TableColumn<DbStructureFactory.ForeignKey, String> tcForeignKeyCatalog;
  @FXML
  private TableColumn<DbStructureFactory.ForeignKey, String> tcForeignKeySchema;
  @FXML
  private TableColumn<DbStructureFactory.ForeignKey, String> tcForeignKeyTable;
  @FXML
  private TableColumn<DbStructureFactory.ForeignKey, String> tcForeignKeyPKColumns;
  @FXML
  private TableColumn<DbStructureFactory.ForeignKey, String> tcForeignKeyFKColumns;
  @FXML
  private TableColumn<DbStructureFactory.ForeignKey, String> tcForeignKeyUpdate;
  @FXML
  private TableColumn<DbStructureFactory.ForeignKey, String> tcForeignKeyDelete;

  private ObjectProperty<TableDesc> tableDescProperty = new SimpleObjectProperty<>();

  public DbTableStructureFrmController() {
    tableDescProperty.addListener((observable, oldValue, newValue) -> {
      generalSet(newValue);
      columnsSet(newValue);
      foreignKeysSet(newValue);
    });
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    tcColumnsPK.setCellValueFactory(cell -> new SimpleBooleanProperty(cell.getValue().isPK()));
    tcColumnsName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
    tcColumnsComment.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLabel()));
    tcColumnsType.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getDataType()));
    tcColumnsNull.setCellValueFactory(cell -> new SimpleBooleanProperty(cell.getValue().isNullable()));
    tcColumnsLength.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getSize()));
    tcColumnsScale.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getScale()));
    tcColumnsDefault.setCellValueFactory(cell -> new SimpleObjectProperty<>("TODO"));
    tcColumnsEditable.setCellValueFactory(cell -> new SimpleBooleanProperty(cell.getValue().isEditable()));

    tcForeignKeyInOut.setCellValueFactory(cell -> new SimpleStringProperty("TODO"));
    tcForeignKeyName.setCellValueFactory(cell -> new SimpleStringProperty("TODO"));
    tcForeignKeyCatalog.setCellValueFactory(cell -> new SimpleStringProperty("TODO"));
    tcForeignKeySchema.setCellValueFactory(cell -> new SimpleStringProperty("TODO"));
    tcForeignKeyTable.setCellValueFactory(cell -> new SimpleStringProperty("TODO"));
    tcForeignKeyPKColumns.setCellValueFactory(cell -> new SimpleStringProperty("TODO"));
    tcForeignKeyFKColumns.setCellValueFactory(cell -> new SimpleStringProperty("TODO"));
    tcForeignKeyUpdate.setCellValueFactory(cell -> new SimpleStringProperty("TODO"));
    tcForeignKeyDelete.setCellValueFactory(cell -> new SimpleStringProperty("TODO"));

    generalSet(null);
    columnsSet(null);
    foreignKeysSet(null);
  }

  /** Set values from table description to general part */
  private void generalSet(TableDesc tableDesc) {
    if (tableDesc == null) {
      lTableName.setText("");
      lQualifiedName.setText("");
      lTableType.setText("");
      taComment.setText("");
    } else {
      lTableName.setText(tableDesc.getName());
      lQualifiedName.setText(String.format("\"%s\".\"%s\".\"%s\"",
          tableDesc.getSchema().getCatalog().getName(),
          tableDesc.getSchema().getName(),
          tableDesc.getName()));
      lTableType.setText(tableDesc.getTableType().name());
      taComment.setText(tableDesc.getName());
    }
  }

  /** Set columns for columns part */
  private void columnsSet(TableDesc tableDesc) {
    tvColumns.getItems().clear();
    if (tableDesc != null) {
      tvColumns.getItems().addAll(tableDesc.getColumns());
    }
  }

  /** Set columns for foreign keys part */
  private void foreignKeysSet(TableDesc tableDesc) {
    tvForeignKeys.getItems().clear();
    if (tableDesc != null) {
      tvForeignKeys.getItems().addAll(tableDesc.getForeignKeys());
    }
  }

  /** Set table description which is currently showed */
  public void setTableDesc(TableDesc tableDesc) {
    tableDescProperty.setValue(tableDesc);
  }

  @SuppressWarnings("unused")
  public TableDesc getTableDesc() { return tableDescProperty.get(); }
  @SuppressWarnings("unused")
  public ObjectProperty<TableDesc> tableDescProperty() { return tableDescProperty; }

  /** Create new instance return main node and controller of this node and sub-nodes */
  public static Tuple2<Parent, DbTableStructureFrmController> createNewInstance() {
    URL resource = DbTableStructureFrmController.class.getResource(FXML_RESOURCE);
    try {
      FXMLLoader loader = new FXMLLoader();
      loader.setLocation(resource);
      loader.setBuilderFactory(new JavaFXBuilderFactory());
      Parent node = loader.load(resource.openStream());
      DbTableStructureFrmController controller = loader.getController();
      return new Tuple2<>(node, controller);
    } catch (IOException e) {
      LOG.error("Problem with reading FXML", e);
      throw new RuntimeException("Problem with reading FXML", e);
    }
  }
}
