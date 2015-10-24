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

import cz.lbenda.common.Constants;
import cz.lbenda.common.Tuple2;
import cz.lbenda.dataman.db.DbConfig;
import cz.lbenda.dataman.db.SQLQueryRows;
import cz.lbenda.dataman.db.TableDesc;
import cz.lbenda.dataman.db.frm.*;
import cz.lbenda.dataman.db.handler.*;
import cz.lbenda.dataman.rc.frm.AboutApplicationHandler;
import cz.lbenda.rcp.DialogHelper;
import cz.lbenda.rcp.ExceptionMessageFrmController;
import cz.lbenda.rcp.IconFactory;
import cz.lbenda.rcp.StatusHelper;
import cz.lbenda.rcp.action.SavableRegistry;
import cz.lbenda.rcp.ribbon.Ribbon;
import cz.lbenda.dataman.db.sql.SQLEditorController;
import cz.lbenda.rcp.config.ConfigurationRW;
import cz.lbenda.rcp.localization.MessageFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.apache.commons.cli.*;
import org.controlsfx.control.StatusBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Iterator;
import java.util.PropertyResourceBundle;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 6.9.15.
 * Stand alone application for dataman */
public class DatamanApp extends Application {

  private static final Logger LOG = LoggerFactory.getLogger(DatamanApp.class);

  private static CommandLine commandLine(String[] args) {
    Options options = new Options();
    options.addOption("mode", true, "Application mode: development, test");
    options.addOption("i", "script", true, "Input script which is executes");
    options.addOption("s", "scripts", true, "Input scripts which is executes separated by comma");
    options.addOption("c", "config", true, "Name of configuration on which is executed script");
    options.addOption("help", "h", false, "Show this help");
    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine cmd = parser.parse(options, args);
      if (cmd.hasOption("h")) { (new HelpFormatter()).printHelp("dataman", options); }
      if (cmd.hasOption("mode")) {
        if ("DEVELOPMENT".equals(cmd.getOptionValue("mode").toUpperCase())) {
          Constants.IS_IN_DEVELOP_MODE = true;
        } else if ("TEST".equals(cmd.getOptionValue("mode").toUpperCase())) {
          Constants.IS_IN_DEVELOP_MODE = true;
        }
      }
      cz.lbenda.dataman.Constants.HEADLESS = cmd.hasOption("i") || cmd.hasOption("s");
      return cmd;
    } catch (ParseException e) {
      LOG.error("Problem with parse command line arguments", e);
      (new HelpFormatter()).printHelp("dataman", options);
      System.exit(1);
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {
    CommandLine cmd = commandLine(args);
    ConfigurationRW.createInstance("dataman", null);
    PropertyResourceBundle prb = null;
    try {
      prb = new PropertyResourceBundle(DatamanApp.class.getResourceAsStream("messages.properties"));
    } catch (IOException e) {
      LOG.error("The messages wasn't loaded", e);
    }
    MessageFactory.createInstance(prb);
    MessageFactory.getInstance().initializePackage("cz.lbenda");

    if (cz.lbenda.dataman.Constants.HEADLESS) {
      DatamanHeadless headless = new DatamanHeadless();
      headless.launch(cmd);
    } else {
      launch(args);
    }
  }

  private BorderPane mainPane;
  private StackPane leftPane = new StackPane();
  private StackPane centerPane = new StackPane();
  private Ribbon ribbon;
  private SQLEditorController te;
  private ObjectProperty<DataTableView> tableViewObjectProperty = new SimpleObjectProperty<>();
  private TabPane centerTabs = new TabPane();
  private TabPane detailTabs = new TabPane();
  private Accordion rightPane = new Accordion();
  private ObjectProperty<SQLQueryRows> sqlQueryRowsObjectProperty = new SimpleObjectProperty<>();
  private StatusBar statusBar = new StatusBar();

  private NodeShower nodeShower = new NodeShower() {
    @Override public void addNode(Node node, String title, boolean closable) { addRemoveToDetail(title, node, closable); }
    @Override public void removeNode(Node node) { addRemoveToDetail("", node, false); }
    @Override public void focusNode(Node node) { DatamanApp.this.focusNode(node); }
  };


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
    mainPane.setBottom(statusBar);

    mainPane.setTop(ribbon);

    SplitPane spHorizontal = new SplitPane();
    spHorizontal.setOrientation(Orientation.HORIZONTAL);
    SplitPane spVertical = new SplitPane();
    spVertical.setOrientation(Orientation.VERTICAL);

    spHorizontal.getItems().addAll(leftPane, spVertical, rightPane);
    spHorizontal.setDividerPositions(0.1f, 0.8f, 0.1f);
    spVertical.getItems().addAll(centerPane, detailTabs);
    spVertical.setDividerPositions(0.8f, 0.2f);

    mainPane.setCenter(spHorizontal);

    centerPane.getChildren().add(centerTabs);

    centerTabs.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      Node n = newValue.getContent();
      if (n instanceof DataTableView) {
        tableViewObjectProperty.setValue((DataTableView) n);
        sqlQueryRowsObjectProperty.setValue(((DataTableView) n).getSqlQueryRows());
      } else {
        tableViewObjectProperty.setValue(null);
        sqlQueryRowsObjectProperty.setValue(null);
      }
    });
    detailTabs.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      Node n = newValue.getContent();
      if (n instanceof DataTableView) {
        sqlQueryRowsObjectProperty.setValue(((DataTableView) n).getSqlQueryRows());
      } else {
        sqlQueryRowsObjectProperty.setValue(null);
      }
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

  /** Add new pane to right */
  public TitledPane addToRight(String title, Node node) {
    TitledPane titlePane = new TitledPane();
    titlePane.setContent(node);
    titlePane.setText(title);
    rightPane.getPanes().add(titlePane);
    return titlePane;
  }

  private void focusNode(@Nonnull Node node) {
    detailTabs.getTabs().stream().filter(tab -> node.equals(tab.getContent()))
        .forEach(tab -> this.detailTabs.getSelectionModel().select(tab));
  }

  /** Add node to center pane */
  private void addRemoveToDetail(@Nonnull String title, @Nonnull Node node, boolean closable) {
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
    StatusHelper.getInstance().setStatusBar(statusBar);
    Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) ->  ExceptionMessageFrmController
        .showException(MessageFactory.getInstance().getMessage("UncaughtException"), throwable));

    ribbon = new Ribbon(MessageFactory.getInstance().getMessage("app.name"),
        IconFactory.getInstance().image(this, "dataman.png", IconFactory.IconLocation.APP_ICON));

    primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("dataman16.png")));
    primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("dataman32.png")));
    primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("dataman48.png")));
    primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("dataman64.png")));
    primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("dataman128.png")));
    primaryStage.setTitle(MessageFactory.getInstance().getMessage("app.name"));

    DbConfigFactory.reloadConfiguration();
    ObjectProperty<DbConfig> currentDbProperty = new SimpleObjectProperty<>();
    ObjectProperty<TableDesc> selectedTableDescProperty = new SimpleObjectProperty<>();

    prepareMainPane();
    ribbon.itemsProperty().addAll(
        new AddDatabaseHandler(),
        new ImportDatabaseHandler(),
        new ExportDatabaseHandler(currentDbProperty),
        new DbConfigMenuOptions(currentDbProperty),
        new ConnectDatabaseHandler(currentDbProperty),
        new EditDatabaseHandler(currentDbProperty),
        new CopyDatabaseHandler(currentDbProperty),
        new RemoveDatabaseHandler(currentDbProperty),
        new ReloadDatabaseHandler(currentDbProperty),
        new RemoveRowsHandler(tableViewObjectProperty),
        new AddRowHandler(tableViewObjectProperty),
        new ReloadTableHandler(tableViewObjectProperty),
        new SaveTableHandler(tableViewObjectProperty),
        new SaveAllTableHandler(currentDbProperty),
        new OpenConnectedTablesHandler(tableViewObjectProperty,
            detailDescriptor -> addRemoveToDetail(detailDescriptor.getTitle(), detailDescriptor.getNode(), detailDescriptor.getClosable())),
        new ExportTableHandler(sqlQueryRowsObjectProperty),
        new ExportTableWithTemplateHandler(sqlQueryRowsObjectProperty),
        new AboutApplicationHandler());

    Scene scene = new Scene(mainPane);
    te = new SQLEditorController(ribbon::addItem, scene, currentDbProperty, nodeShower);

    addToCenter(SQLEditorController.WINDOW_TITLE, te.getNode(), false);

    Tuple2<Parent, DbTableStructureFrmController> dbTableStructureFrmController
        = DbTableStructureFrmController.createNewInstance();
    addToCenter(DbTableStructureFrmController.WINDOW_TITLE, dbTableStructureFrmController.get1(), false);
    selectedTableDescProperty.addListener((observable, oldValue, newValue) ->
        dbTableStructureFrmController.get2().setTableDesc(newValue));


    DbStructureFrmController dfc = new DbStructureFrmController(currentDbProperty, td -> new Thread(() -> {
      StatusHelper.getInstance().progressStart(td, DataTableFrmController.TASK_NAME, 2);
      StatusHelper.getInstance().progressNextStep(td, td.getName(), 0);
      DataTableFrmController controller = new DataTableFrmController(td);
      StatusHelper.getInstance().progressNextStep(td, td.getName(), 0);
      Platform.runLater(() -> addToCenter(controller.titleProperty(), controller.getTabView(), true));
      StatusHelper.getInstance().progressFinish(td, DataTableFrmController.STEP_FINISH);
    }).start(), selectedTableDescProperty);
    leftPane.getChildren().add(dfc.getControlledNode());

    RowEditorFrmController rowEditorFrmController = new RowEditorFrmController(tableViewObjectProperty);
    addToRight(RowEditorFrmController.WINDOW_TITLE, rowEditorFrmController.getPane());

    // Scene scene = te.createScene();
    primaryStage.setScene(scene);
    primaryStage.setOnCloseRequest(event -> {
      if (!DialogHelper.getInstance().showUnsavedObjectDialog(SavableRegistry.getInstance())) {
        event.consume();
      }
    });
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
