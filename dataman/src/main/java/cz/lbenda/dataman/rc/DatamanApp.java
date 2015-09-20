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

import cz.lbenda.dataman.db.frm.DataTableFrmController;
import cz.lbenda.dataman.db.frm.DataTableView;
import cz.lbenda.dataman.db.frm.DbStructureFrmController;
import cz.lbenda.dataman.db.handler.*;
import cz.lbenda.rcp.ribbon.RibbonController;
import cz.lbenda.dataman.db.sql.SQLEditorController;
import cz.lbenda.rcp.config.ConfigurationRW;
import cz.lbenda.rcp.localization.MessageFactory;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Iterator;
import java.util.PropertyResourceBundle;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 6.9.15.
 * Stand allone application for dataman */
public class DatamanApp extends Application {

  private static final Logger LOG = LoggerFactory.getLogger(DatamanApp.class);

  public static void main(String[] args) {
    ConfigurationRW.createInstance("dataman", null);
    PropertyResourceBundle prb = null;
    try {
      prb = new PropertyResourceBundle(DatamanApp.class.getResourceAsStream("messages.properties"));
    } catch (IOException e) {
      LOG.error("The messages wasn't loaded", e);
    }
    MessageFactory.createInstanance(prb);
    launch(args);
  }

  private BorderPane mainPane;
  private StackPane leftPane = new StackPane();
  private StackPane centerPane = new StackPane();
  private RibbonController ribbonController;
  private SQLEditorController te;
  ObjectProperty<DataTableView> tableViewObjectProperty = new SimpleObjectProperty<>();
  private TabPane centerTabs = new TabPane();
  private TabPane detailTabs = new TabPane();

  // TextEditor te  = new TextEditor();

  public void prepareMainPane() {
    mainPane = new BorderPane();
    mainPane.setId("mainPane");
    mainPane.setMaxHeight(-1);
    mainPane.setMaxWidth(-1);
    mainPane.setMinHeight(-1);
    mainPane.setMinWidth(-1);
    mainPane.setPrefHeight(800.0);
    mainPane.setPrefWidth(1024.0);
    mainPane.getStyleClass().add("background");

    ribbonController = new RibbonController(MessageFactory.getInstance());
    mainPane.setTop(ribbonController.getRibbon());

    SplitPane spHorizontal = new SplitPane();
    spHorizontal.setOrientation(Orientation.HORIZONTAL);
    SplitPane spVertical = new SplitPane();
    spVertical.setOrientation(Orientation.VERTICAL);

    spHorizontal.getItems().addAll(leftPane, spVertical);
    spHorizontal.setDividerPositions(0.2f, 0.8f);
    spVertical.getItems().addAll(centerPane, detailTabs);
    spVertical.setDividerPositions(0.8f, 0.2f);

    mainPane.setCenter(spHorizontal);

    centerPane.getChildren().add(centerTabs);

    centerTabs.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
      Node n = newValue.getContent();
      if (n instanceof DataTableView) {
        tableViewObjectProperty.setValue((DataTableView) n);
      } else { tableViewObjectProperty.setValue(null); }
    });
  }

  /** Add node to center pane */
  public Tab addToCenter(String title, Node node, boolean closable) {
    Tab tab = new Tab(title, node);
    tab.setClosable(closable);
    this.centerTabs.getTabs().add(tab);
    this.centerTabs.getSelectionModel().select(tab);
    return tab;
  }

  /** Add node to center pane */
  public Tab addToCenter(ObjectProperty<String> title, Node node, boolean closable) {
    Tab tab = addToCenter(title.getValue(), node, closable);
    title.addListener((observable, oldValue, newValue) -> tab.setText(newValue));
    return tab;
  }

  /** Add node to center pane */
  public void addRemoveToDetail(@Nonnull String title, @Nonnull Node node, boolean closable) {
    boolean removed = false;
    for (Iterator<Tab> itt = detailTabs.getTabs().iterator(); itt.hasNext(); ) {
      Tab tab = itt.next();
      if (node.equals(tab.getContent())) {
        itt.remove();
        removed = true;
      }
    }
    if (!removed) {
      Tab tab = new Tab(title, node);
      tab.setClosable(closable);
      this.detailTabs.getTabs().add(tab);
      this.detailTabs.getSelectionModel().select(tab);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void start(Stage primaryStage) throws Exception {
    primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("dataman_16.png")));
    primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("dataman_32.png")));
    primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("dataman_48.png")));
    primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("dataman_64.png")));
    primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("dataman_128.png")));
    primaryStage.setTitle(MessageFactory.getInstance().getMessage("app.name"));

    DbConfigFactory.reloadConfiguration();
    ObjectProperty<DbConfig> currentDbProperty = new SimpleObjectProperty<>();

    prepareMainPane();
    ribbonController.getRibbon().getItemFactory().getItemsHandler().add(new AddDatabaseHandler());
    ribbonController.getRibbon().getItemFactory().getItemsHandler().add(new ImportDatabaseHandler());
    ribbonController.getRibbon().getItemFactory().getItemsHandler().add(new ExportDatabaseHandler(currentDbProperty));
    ribbonController.getRibbon().getItemFactory().getItemsHandler().add(new DbConfigMenuOptions(currentDbProperty));
    ribbonController.getRibbon().getItemFactory().getItemsHandler().add(new ConnectDatabaseHandler(currentDbProperty));
    ribbonController.getRibbon().getItemFactory().getItemsHandler().add(new EditDatabaseHandler(currentDbProperty));
    ribbonController.getRibbon().getItemFactory().getItemsHandler().add(new CopyDatabaseHandler(currentDbProperty));
    ribbonController.getRibbon().getItemFactory().getItemsHandler().add(new RemoveDatabaseHandler(currentDbProperty));
    ribbonController.getRibbon().getItemFactory().getItemsHandler().add(new RemoveRowsHandler(tableViewObjectProperty));
    ribbonController.getRibbon().getItemFactory().getItemsHandler().add(new AddRowHandler(tableViewObjectProperty));
    ribbonController.getRibbon().getItemFactory().getItemsHandler().add(new ReloadTableHandler(tableViewObjectProperty));
    ribbonController.getRibbon().getItemFactory().getItemsHandler().add(new SaveTableHandler(tableViewObjectProperty));
    ribbonController.getRibbon().getItemFactory().getItemsHandler().add(new OpenConnectedTablesHandler(tableViewObjectProperty,
        detailDescriptor -> addRemoveToDetail(detailDescriptor.getTitle(), detailDescriptor.getNode(), detailDescriptor.getClosable())));

    Scene scene = new Scene(mainPane);
    te = new SQLEditorController(ribbonController.getRibbon().getItemFactory(), scene, currentDbProperty,
      detailDescriptor -> addRemoveToDetail(detailDescriptor.getTitle(), detailDescriptor.getNode(), detailDescriptor.getClosable()));

    addToCenter("SQL", te.getNode(), false);

    DbStructureFrmController dfc = new DbStructureFrmController(currentDbProperty, td -> {
      DataTableFrmController controller = new DataTableFrmController(td);
      addToCenter(controller.titleProperty(), controller.getTabView(), true);
    });
    leftPane.getChildren().add(dfc.getControlledNode());

    // Scene scene = te.createScene();
    primaryStage.setScene(scene);
    primaryStage.show();

    /*
    try {
      // AquaFx.style();
      // FlatterFX.style();
      //AeroFX.style();
    } catch (Exception e) {
      LOG.error("Problem with switch to AquaFx style", e);
    }
    */
  }

  @Override
  public void stop() {
    te.stop();
  }
}
