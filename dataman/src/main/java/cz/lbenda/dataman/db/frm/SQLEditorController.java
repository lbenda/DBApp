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

import cz.lbenda.dataman.db.SQLQueryResult;
import cz.lbenda.dataman.db.handler.SQLRunHandler;
import cz.lbenda.dataman.rc.DbConfig;
import cz.lbenda.dataman.rc.DetailDescriptor;
import cz.lbenda.gui.editor.HighlighterSQL;
import cz.lbenda.gui.editor.TextEditor;
import cz.lbenda.rcp.localization.Message;
import cz.lbenda.rcp.ribbon.RibbonItemFactory;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.DoubleSummaryStatistics;
import java.util.function.Consumer;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 6.9.15.
 * Text editor with another part for SQL editing and getting result */
public class SQLEditorController {

  private static Logger LOG = LoggerFactory.getLogger(SQLEditorController.class);
  private static String HTML;

  private static final String MAIN_PANE = "mainPane";

  static {
    try {
      HTML = IOUtils.toString(SQLEditorController.class.getResourceAsStream("SQLConsoleHTML.html"));
    } catch (IOException e) {
      LOG.error("Problem with read SQLConsoleHTML.html", e);
      HTML = "<html><head></head><body>%s</body></html>";
    }
  }

  @Message
  private String msgConsoleTitle = "Console";

  private TextEditor textEditor = new TextEditor();
  private WebView webView = new WebView(); public WebView getWebView() { return webView; }
  private StringBuffer consoleMessages = new StringBuffer();
  private ObjectProperty<DbConfig> dbConfigProperty;
  private Consumer<DetailDescriptor> detailAppender;

  /** Change text in editor */
  public void changeText(String text) { textEditor.changeText(text); }
  /** Return text which is in code Area */
  public String getText() { return textEditor.getText(); }
  /** Return text which will be executed. */
  public String[] getExecutedText() {
    String text = textEditor.getSelectedText();
    if (text == null || "".equals(text)) { text = textEditor.getText(); }
    if (StringUtils.isBlank(text)) { return new String[0]; }
    String[] lines = text.split("\n");
    StringBuilder sb = new StringBuilder();
    for (String line : lines) { sb.append(line.trim()).append("\n"); }
    return sb.toString().split(";\n");
  }

  private AnchorPane node = new AnchorPane();

  /** Return controlled node */
  public Node getNode() {
    return node;
  }

  public SQLEditorController(RibbonItemFactory actionFactory, Scene scene, ObjectProperty<DbConfig> dbConfigProperty,
                             Consumer<DetailDescriptor> detailAppender) {
    node.setMaxHeight(Double.MAX_VALUE);
    node.setMaxHeight(Double.MAX_VALUE);

    this.detailAppender = detailAppender;
    this.dbConfigProperty = dbConfigProperty;

    textEditor.setScene(scene);
    textEditor.changeHighlighter(new HighlighterSQL());

    detailAppender.accept(new DetailDescriptor(msgConsoleTitle, webView, false));
    CodeArea ca = textEditor.createCodeArea();
    ca.setMaxHeight(Double.MAX_VALUE);
    ca.setMaxWidth(Double.MAX_VALUE);
    AnchorPane.setTopAnchor(ca, 0.0);
    AnchorPane.setBottomAnchor(ca, 0.0);
    AnchorPane.setLeftAnchor(ca, 0.0);
    AnchorPane.setRightAnchor(ca, 0.0);
    node.getChildren().add(ca);

    actionFactory.getItemsHandler().add(new SQLRunHandler(dbConfigProperty, this));
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
      DataTableFrmController dataTableController = new DataTableFrmController(result);

      String title = result.getSql() == null ? "" : (result.getSql().length() > 50 ? result.getSql().substring(1, 45) + "..." : result.getSql());
      detailAppender.accept(new DetailDescriptor(title, dataTableController.getTabView(), true));
    }
  }

  /** Stop calculate highlight */
  public void stop() { textEditor.stop(); }
}
