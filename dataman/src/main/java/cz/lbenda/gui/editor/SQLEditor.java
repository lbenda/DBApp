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
package cz.lbenda.gui.editor;

import cz.lbenda.common.Tuple2;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.sql.*;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 6.9.15.
 * Text editor with another part for SQL editing and getting result */
public class SQLEditor {

  private static Logger LOG = LoggerFactory.getLogger(SQLEditor.class);
  private static String HTML;
  private static final String FXML_RESOURCE = "SQLEditorRibbon.fxml";
  private static final String MAIN_PANE = "mainPane";

  static {
    try {
      HTML = IOUtils.toString(SQLEditor.class.getResourceAsStream("SQLConsoleHTML.html"));
    } catch (IOException e) {
      LOG.error("Problem with read SQLConsoleHTML.html", e);
      HTML = "<html><head></head><body>%s</body></html>";
    }
  }

  private TextEditor textEditor = new TextEditor();
  private WebView webView = new WebView(); public WebView getWebView() { return webView; }
  private Scene scene;
  private TabPane tabPane = new TabPane();
  private StringBuffer consoleMessages = new StringBuffer();
  private SQLExecutor sqlExecutor; public SQLExecutor getSqlExecutor() { return sqlExecutor; } public void setSqlExecutor(SQLExecutor sqlExecutor) { this.sqlExecutor = sqlExecutor; }
  private SQLRibbonController ribbonController;

  /** Change text in editor */
  public void changeText(String text) { textEditor.changeText(text); }
  /** Return text which is in code Area */
  public String getText() { return textEditor.getText(); }
  /** Return text which will be executed. */
  public String getExecutedText() { return textEditor.getText(); }

  public SQLEditor(SQLExecutor sqlExecutor) {
    this.sqlExecutor = sqlExecutor;
    SplitPane splitPane = new SplitPane();
    URL resource = getClass().getResource(FXML_RESOURCE);
    BorderPane mainPane = null;
    try {
      FXMLLoader loader = new FXMLLoader();
      loader.setLocation(resource);
      loader.setBuilderFactory(new JavaFXBuilderFactory());

      mainPane = (BorderPane) loader.load(resource.openStream());
      ribbonController = (SQLRibbonController) loader.getController();
      ribbonController.setSqlRunAH(getExecuteQueryHandlerAL());
      scene = new Scene(mainPane);
    } catch (IOException e) {
      LOG.error("Problem with reading ribbon", e);
    }

    textEditor.changeHighlighter(new HighlighterSQL());
    final StackPane sp1 = new StackPane();
    sp1.getChildren().add(textEditor.createCodeArea());
    final StackPane sp2 = new StackPane();
    sp2.getChildren().add(tabPane);

    Tab consoleTab = new Tab();
    consoleTab.setText("Console");
    consoleTab.setContent(webView);
    tabPane.getTabs().add(consoleTab);

    splitPane.setOrientation(Orientation.VERTICAL);
    splitPane.getItems().addAll(sp1, sp2);
    splitPane.setDividerPositions(0.4f, 0.6f);
    if (scene == null) { scene = new Scene(new StackPane(splitPane), 600, 400); }
    else if (mainPane != null) { mainPane.setCenter(splitPane); }
  }

  public void addQueryResult(SQLQueryResult result) {
    StringBuilder msg = new StringBuilder();
    if (result.getSqlQueryRows() == null) {
      msg.append("<sql>").append(result.getSql()).append("</sql>\n");
      if (StringUtils.isNotBlank(result.getErrorMsg())) {
        msg.append("<error>").append(result.getErrorMsg()).append("</error>\n");
      }
      if (result.getAffectedRow() != null) {
        msg.append("Affected rows: ").append(result.getAffectedRow()).append("<br />\n");
      }
      consoleMessages.append("<div class=\"msg\">\n").append(msg).append("</div>\n");
      webView.getEngine().loadContent(String.format(HTML, consoleMessages.toString()));
    } else {
      DataTableController dataTableController = new DataTableController();
      dataTableController.setSqlQueryResult(result);
      Tab gridTab = new Tab();
      String title = result.getSql() == null ? "" : (result.getSql().length() > 50 ? result.getSql().substring(1, 45) + "..." : result.getSql());
      gridTab.setText(title);
      gridTab.setContent(dataTableController.getNode());
      tabPane.getTabs().add(gridTab);
    }
  }

  public Scene createScene() {
    return scene;
  }

  /** Stop calculate highlight */
  public void stop() { textEditor.stop(); }

  private ExecuteQueryHandler executeQueryHandlerAL;

  /** Return action lister which can react on query executed */
  public EventHandler<ActionEvent> getExecuteQueryHandlerAL() {
    if (executeQueryHandlerAL == null) { executeQueryHandlerAL = new ExecuteQueryHandler(this); }
    return executeQueryHandlerAL;
  }

  private static class ExecuteQueryHandler implements EventHandler<ActionEvent> {
    private SQLEditor sqlEditor;
    public ExecuteQueryHandler(SQLEditor sqlEditor) {
      this.sqlEditor = sqlEditor;
    }

    @Override
    public void handle(ActionEvent e) {
      String sql = sqlEditor.getExecutedText(); // TODO split by ;
      SQLQueryResult sqlQueryResult = new SQLQueryResult();
      sqlQueryResult.setSql(sql);
      sqlEditor.getSqlExecutor().onPreparedStatement(sql,
          tuple2 -> this.statementToSQLQueryResult(sqlQueryResult, tuple2));
      sqlEditor.addQueryResult(sqlQueryResult);
    }

    public void statementToSQLQueryResult(SQLQueryResult result, Tuple2<PreparedStatement, SQLException> tuple) {
      if (tuple.get2() != null) {
        result.setErrorMsg(tuple.get2().getMessage());
        LOG.debug(String.format("Problem with execute SQL '%s'", result.getSql()), tuple.get2());
      } else {
        try {
          boolean ex = tuple.get1().execute();
          if (ex) {
            try (ResultSet rs = tuple.get1().getResultSet()) {
              ResultSetMetaData mtd = rs.getMetaData();
              SQLQueryResult.SQLQueryRows sqlRows = new SQLQueryResult.SQLQueryRows();
              result.setSqlQueryRows(sqlRows);
              int columnCount = mtd.getColumnCount();
              SQLQueryResult.SQLQueryColumn columns[] = new SQLQueryResult.SQLQueryColumn[columnCount];
              for (int i = 1; i <= columnCount; i++) {
                columns[i - 1] = new SQLQueryResult.SQLQueryColumn(mtd, i);
              }
              SQLQueryResult.SQLQueryMetaData metaData = new SQLQueryResult.SQLQueryMetaData();
              metaData.setColumns(columns);
              sqlRows.setMetaData(metaData);
              ObservableList<SQLQueryResult.SQLQueryRow> rows = FXCollections.observableArrayList();
              while (rs.next()) {
                Object[] values = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) { values[i - 1] = rs.getObject(i); }
                SQLQueryResult.SQLQueryRow row = new SQLQueryResult.SQLQueryRow();
                row.setValues(values);
                rows.add(row);
              }
              sqlRows.setRows(rows);
            }
          } else {
            result.setAffectedRow(tuple.get1().getUpdateCount());
          }
        } catch (SQLException e) {
          result.setErrorMsg(e.getMessage());
          LOG.debug(String.format("Problem with execute SQL '%s'", result.getSql()), e);
        }
      }
    }
  }
}
