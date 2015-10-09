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
import cz.lbenda.dataman.db.dialect.ColumnType;
import cz.lbenda.rcp.IconFactory;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 12.9.15.
 * Controller for frame which show structure of database */
@SuppressWarnings("unchecked")
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

  private final static Image imageArray = IconFactory.getInstance().image(DbStructureFrmController.class, "array.png",
      IconFactory.IconLocation.INDICATOR);
  private final static Image imageBoolean = IconFactory.getInstance().image(DbStructureFrmController.class, "boolean.png",
      IconFactory.IconLocation.INDICATOR);
  private final static Image imageCharacter = IconFactory.getInstance().image(DbStructureFrmController.class, "character.png",
      IconFactory.IconLocation.INDICATOR);
  private final static Image imageTime = IconFactory.getInstance().image(DbStructureFrmController.class, "clock.png",
      IconFactory.IconLocation.INDICATOR);
  private final static Image imageDate = IconFactory.getInstance().image(DbStructureFrmController.class, "date.png",
      IconFactory.IconLocation.INDICATOR);
  private final static Image imageDecimal = IconFactory.getInstance().image(DbStructureFrmController.class, "decimal.png",
      IconFactory.IconLocation.INDICATOR);
  private final static Image imageHash = IconFactory.getInstance().image(DbStructureFrmController.class, "hash.png",
      IconFactory.IconLocation.INDICATOR);
  private final static Image imageInteger = IconFactory.getInstance().image(DbStructureFrmController.class, "integer.png",
      IconFactory.IconLocation.INDICATOR);
  private final static Image imageUUID = IconFactory.getInstance().image(DbStructureFrmController.class, "uuid.png",
      IconFactory.IconLocation.INDICATOR);

  public Node getControlledNode() { return treeView; }

  private TreeView treeView;
  private ObjectProperty<DbConfig> dbConfigProperty;
  private final ListChangeListener<CatalogDesc> catalogChangeListener = change -> {
    while (change.next()) {
      if (change.wasAdded()) {
        TreeItem root = new TreeItem();
        treeView.setRoot(root);
        DbConfig dbConfig = dbConfigProperty.getValue();
        createCatalogTreeItems(root, dbConfig);
      }
    }
  };

  public DbStructureFrmController(@Nonnull ObjectProperty<DbConfig> dbConfigProperty,
                                  @Nonnull Consumer<TableDesc> tableShower) {
    this.dbConfigProperty = dbConfigProperty;
    treeView = new TreeView<>();
    treeView.setShowRoot(false);
    treeView.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        TreeItem<TableDesc> item = (TreeItem<TableDesc>) treeView.getSelectionModel().getSelectedItem();
        if (item.getValue() instanceof TableDesc || item.getValue() == null) {
          tableShower.accept(item.getValue());
        }
      }
    });
    treeView.setOnDragDetected(event -> {
      if (event.getSource() instanceof TreeView) {
        TreeView source = (TreeView) event.getSource();
        TreeItem item = (TreeItem<TableDesc>) source.getSelectionModel().getSelectedItem();
        if (item == null) { return; }
        Dragboard db = treeView.startDragAndDrop(TransferMode.ANY);

        ClipboardContent content = new ClipboardContent();
        if (item.getValue() instanceof SchemaDesc) {
          content.putString("\"" + ((SchemaDesc) item.getValue()).getName() + "\"");
        } else if (item.getValue() instanceof TableDesc) {
          TableDesc td = (TableDesc) item.getValue();
          content.putString("\"" + td.getSchema().getName() + "\".\"" + td.getName() + "\"");
        } else if (item.getValue() instanceof StringConverterHolder) {
          StringConverterHolder holder = (StringConverterHolder) item.getValue();
          if (holder.getItem() instanceof ColumnDesc) {
            content.putString("\"" + holder.toString() + "\"");
          }
        }
        db.setContent(content);
        event.consume();
      }
    });
    dbConfigProperty.addListener((observable, oldValue, newValue) -> {
      if (oldValue != null) {
        oldValue.getCatalogs().removeListener(catalogChangeListener);
      }
      TreeItem root = new TreeItem();
      treeView.setRoot(root);
      root.setExpanded(true);
      if (newValue != null) {
        newValue.getCatalogs().addListener(catalogChangeListener);
        if (newValue.getCatalogs().size() > 0) {
          createCatalogTreeItems(root, newValue);
        }
      }
    });
  }

  private void createCatalogTreeItems(TreeItem root, DbConfig dbConfig) {
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
      HBox hbox = new HBox();
      Image image = null;
      if (columnDesc.isPK()) { image = imagePrimaryKey; }
      if (columnDesc.isFk()) {
        if (image == null) { image = imageForeignKey; }
        else { image = imagePrimaryAndForeignKey; }
      }
      if (image == null) { image = imageBlank; }
      hbox.getChildren().add(new ImageView(image));

      image = null;
      if (columnDesc.getDataType() == ColumnType.BOOLEAN || columnDesc.getDataType() == ColumnType.BIT) { image = imageBoolean; }
      else if (columnDesc.getDataType() == ColumnType.ARRAY || columnDesc.getDataType() == ColumnType.BYTE_ARRAY) { image = imageArray; }
      else if (columnDesc.getDataType() == ColumnType.BYTE
          || columnDesc.getDataType() == ColumnType.SHORT
          || columnDesc.getDataType() == ColumnType.INTEGER
          || columnDesc.getDataType() == ColumnType.LONG
          || (columnDesc.getDataType() == ColumnType.DECIMAL && columnDesc.getScale() == 0)) { image = imageInteger; }
      else if (columnDesc.getDataType() == ColumnType.FLOAT
          || columnDesc.getDataType() == ColumnType.DOUBLE
          || (columnDesc.getDataType() == ColumnType.DECIMAL && columnDesc.getScale() > 0)) { image = imageDecimal; }
      else if (columnDesc.getDataType() == ColumnType.DATE
          || columnDesc.getDataType() == ColumnType.TIMESTAMP) { image = imageDate; }
      else if (columnDesc.getDataType() == ColumnType.TIME) { image = imageTime; }
      else if (columnDesc.getDataType() == ColumnType.STRING
          || columnDesc.getDataType() == ColumnType.CLOB) { image = imageCharacter; }
      else if (columnDesc.getDataType() == ColumnType.BLOB
          || columnDesc.getDataType() == ColumnType.BIT_ARRAY
          || columnDesc.getDataType() == ColumnType.OBJECT) { image = imageHash; }
      else if (columnDesc.getDataType() == ColumnType.UUID) { image = imageUUID; }
      else {
        image = imageBlank;
        System.out.println(columnDesc.getDataType());
      }
      hbox.getChildren().add(new ImageView(image));

      columnItem = new TreeItem<>(new StringConverterHolder<>(columnDesc, columnDesc::getName), hbox);
      //noinspection unchecked
      item.getChildren().add(columnItem);
    });
  }
}
