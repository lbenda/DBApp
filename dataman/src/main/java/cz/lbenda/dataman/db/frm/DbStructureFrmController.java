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

import cz.lbenda.dataman.db.TableDesc;
import cz.lbenda.dataman.rc.DbConfig;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.function.Consumer;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 12.9.15.
 * Controller for frame which show structure of database */
public class DbStructureFrmController {

  private final Image imageTable = new Image(getClass().getResourceAsStream("table.png"));
  private final Image imageView = new Image(getClass().getResourceAsStream("view.png"));

  public Node getControlledNode() { return treeView; }

  private TreeView treeView;

  @SuppressWarnings("unchecked")
  public DbStructureFrmController(ObjectProperty<DbConfig> dbConfigProperty, Consumer<TableDesc> tableShower) {
    treeView = new TreeView<>();
    treeView.setShowRoot(false);
    treeView.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        TreeItem<TableDesc> item = (TreeItem<TableDesc>) treeView.getSelectionModel().getSelectedItem();
        tableShower.accept(item.getValue());
      }
    });
    dbConfigProperty.addListener(observable -> {
      DbConfig dbConfig = dbConfigProperty.getValue();
      TreeItem<String> root = new TreeItem<>();
      treeView.setRoot(root);
      root.setExpanded(true);

      if (dbConfig != null && dbConfig.isConnected()) {
        createCatalogTreeItems(root, dbConfig);
      }
    });
  }

  private void createCatalogTreeItems(TreeItem<String> root, DbConfig dbConfig) {
    dbConfig.getCatalogs().forEach(catalog -> {
      if (dbConfig.isShowCatalog(catalog)) {
        TreeItem<String> catalogItem = new TreeItem<>(catalog);
        root.getChildren().add(catalogItem);
        createSchemasTreeItems(catalogItem, dbConfig, catalog);
      } else {
        createSchemasTreeItems(root, dbConfig, catalog);
      }
    });
  }

  private void createSchemasTreeItems(TreeItem<String> item, DbConfig dbConfig, String catalog) {
    dbConfig.getSchemas(catalog).forEach(schema -> {
      if (dbConfig.isShowSchema(catalog, schema)) {
        TreeItem<String> schemaItem = new TreeItem<>(schema);
        item.getChildren().add(schemaItem);
        createTableTypesTreeItems(schemaItem, dbConfig, catalog, schema);
      } else { createTableTypesTreeItems(item, dbConfig, catalog, schema); }
    });
  }

  private void createTableTypesTreeItems(TreeItem<String> item, DbConfig dbConfig, String catalog, String schema) {
    dbConfig.shownTableType(catalog, schema).forEach(tableType -> {
      final TreeItem<String> ttItem;
      ImageView iv = null;
      switch (tableType) {
        case TABLE : iv = new ImageView(imageTable); break;
        case VIEW : iv = new ImageView(imageView); break;
      }
      if (iv == null) { ttItem = new TreeItem<>(tableType.name()); }
      else { ttItem = new TreeItem<>(tableType.name(), iv); }
      item.getChildren().add(ttItem);
      createTablesTreeItems(ttItem, dbConfig, catalog, schema, tableType);
    });
  }

  @SuppressWarnings("unchecked")
  private void createTablesTreeItems(TreeItem item, DbConfig dbConfig, String catalog, String schema, TableDesc.TableType tableType) {
    final Image image;
    switch (tableType) {
      case TABLE : image = imageTable; break;
      case VIEW : image = imageView; break;
      default: image = null;
    }
    dbConfig.getTableDescriptions(catalog, schema, tableType).stream().sorted(TableDesc::compareTo).forEach(td -> {
      if (dbConfig.isShowTable(td)) {
        final TreeItem<TableDesc> tableItem; // TODO apped tooltim from table comment
        if (image != null) { tableItem = new TreeItem<>(td, new ImageView(image)); }
        else { tableItem = new TreeItem<>(td); }
        item.getChildren().add(tableItem);
      }
    });
  }
}
