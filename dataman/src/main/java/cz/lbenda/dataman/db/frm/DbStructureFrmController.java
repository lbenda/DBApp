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

import cz.lbenda.common.StringConverterHolder;
import cz.lbenda.dataman.db.*;
import cz.lbenda.rcp.IconFactory;
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

  private final static Image imageBlank = IconFactory.getInstance().image(DbStructureFrmController.class, "blank.png",
      IconFactory.IconLocation.INDICATOR);
  private final static Image imageTable = IconFactory.getInstance().image(DbStructureFrmController.class, "table.png",
      IconFactory.IconLocation.INDICATOR);
  private final static Image imageView = IconFactory.getInstance().image(DbStructureFrmController.class, "view.png",
      IconFactory.IconLocation.INDICATOR);
  private final static Image imagePrimaryKey = IconFactory.getInstance().image(DbStructureFrmController.class, "key-golden.png",
      IconFactory.IconLocation.INDICATOR);
  private final static Image imageForeignKey = IconFactory.getInstance().image(DbStructureFrmController.class, "key-blue.png",
      IconFactory.IconLocation.INDICATOR);
  private final static Image imagePrimaryAndForeignKey = IconFactory.getInstance().image(DbStructureFrmController.class, "key-goldenAndBlue.png",
      IconFactory.IconLocation.INDICATOR);

  public Node getControlledNode() { return treeView; }

  private TreeView treeView;

  @SuppressWarnings("unchecked")
  public DbStructureFrmController(ObjectProperty<DbConfig> dbConfigProperty, Consumer<TableDesc> tableShower) {
    treeView = new TreeView<>();
    treeView.setShowRoot(false);
    treeView.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        TreeItem<TableDesc> item = (TreeItem<TableDesc>) treeView.getSelectionModel().getSelectedItem();
        tableShower.accept(item.getValue()); // FIXME Exception in thread "JavaFX Application Thread" java.lang.ClassCastException: java.lang.String cannot be cast to cz.lbenda.dataman.db.TableDesc
      }
    });
    dbConfigProperty.addListener(observable -> {
      DbConfig dbConfig = dbConfigProperty.getValue();
      TreeItem<Object> root = new TreeItem<>();
      treeView.setRoot(root);
      root.setExpanded(true);

      if (dbConfig != null && dbConfig.connectionProvider.isConnected()) {
        createCatalogTreeItems(root, dbConfig);
      }
    });
  }

  private void createCatalogTreeItems(TreeItem<Object> root, DbConfig dbConfig) {
    long showedCatalog = dbConfig.getCatalogs().stream().filter(catalogDesc -> !catalogDesc.isHidden()).count();
    if (showedCatalog > 1) {
      dbConfig.getCatalogs().stream().filter(catalogDesc -> !catalogDesc.isHidden()).forEach(catalog -> {
        TreeItem<Object> catalogItem = new TreeItem<>(catalog);
        root.getChildren().add(catalogItem);
        createSchemasTreeItems(catalogItem, catalog);
      });
    } else {
      dbConfig.getCatalogs().stream().filter(catalogDesc -> !catalogDesc.isHidden()).forEach(catalog ->
          createSchemasTreeItems(root, catalog));
    }
  }

  private void createSchemasTreeItems(TreeItem<Object> item, CatalogDesc catalog) {
    long showedSchemas = catalog.getSchemas().stream().filter(schemaDesc -> !schemaDesc.isHidden()).count();
    catalog.getSchemas().stream().filter(schemaDesc -> !schemaDesc.isHidden()).forEach(schema -> {
      if (showedSchemas > 1) {
        TreeItem<Object> schemaItem = new TreeItem<>(schema);
        item.getChildren().add(schemaItem);
        createTableTypesTreeItems(schemaItem, schema);
      } else {
        createTableTypesTreeItems(item, schema);
      }
    });
  }

  private void createTableTypesTreeItems(TreeItem<Object> item, SchemaDesc schema) {
    schema.allTableTypes().forEach(tableType -> {
      final TreeItem<Object> ttItem;
      ImageView iv = null;
      switch (tableType) {
        case TABLE:
          iv = new ImageView(imageTable);
          break;
        case VIEW:
          iv = new ImageView(imageView);
          break;
      }
      if (iv == null) {
        ttItem = new TreeItem<>(tableType.name());
      } else {
        ttItem = new TreeItem<>(tableType.name(), iv);
      }
      item.getChildren().add(ttItem);
      createTablesTreeItems(ttItem, schema, tableType);
    });
  }

  @SuppressWarnings("unchecked")
  private void createTablesTreeItems(TreeItem item, SchemaDesc schema, TableDesc.TableType tableType) {
    final Image image;
    switch (tableType) {
      case TABLE : image = imageTable; break;
      case VIEW : image = imageView; break;
      default: image = imageBlank;
    }
    schema.tablesByType(tableType).stream().sorted(TableDesc::compareTo).forEach(td -> {
      if (!td.isHidden()) {
        final TreeItem<TableDesc> tableItem; // TODO append tooltip from table comment
        tableItem = new TreeItem<>(td, new ImageView(image));
        item.getChildren().add(tableItem);
        createColumnTreeItems(tableItem, td);
      }
    });
  }

  private void createColumnTreeItems(TreeItem item, TableDesc tableDesc) {
    tableDesc.getColumns().forEach(columnDesc -> {
      final TreeItem columnItem; // TODO append tooltip from table comment
      Image image = null;
      if (columnDesc.isPK()) { image = imagePrimaryKey; }
      if (columnDesc.isFk()) {
        if (image == null) { image = imageForeignKey; }
        else { image = imagePrimaryAndForeignKey; }
      }
      if (image == null) { image = imageBlank; }
      columnItem = new TreeItem<>(new StringConverterHolder<>(columnDesc, columnDesc::getName), new ImageView(image));
      //noinspection unchecked
      item.getChildren().add(columnItem);
    });
  }
}
