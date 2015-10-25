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
package cz.lbenda.dataman.db.sql;

import cz.lbenda.dataman.db.SQLQueryResult;
import cz.lbenda.dataman.db.frm.DataTableFrmController;
import cz.lbenda.dataman.db.DbConfig;
import cz.lbenda.dataman.rc.NodeShower;
import cz.lbenda.gui.editor.HighlighterSQL;
import cz.lbenda.gui.editor.TextEditor;
import cz.lbenda.rcp.ExceptionMessageFrmController;
import cz.lbenda.rcp.localization.Message;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Consumer;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 6.9.15.
 * Text editor with another part for SQL editing and getting result */
public class SQLEditorController implements SQLSExecutor.SQLSExecutorConsumer {

  private static Logger LOG = LoggerFactory.getLogger(SQLEditorController.class);
  private static String HTML;

  @Message
  public final static String WINDOW_TITLE = "SQL";
  @Message
  public final static String msgConsoleTitle = "Console";

  static {
    try {
      HTML = IOUtils.toString(SQLEditorController.class.getResourceAsStream("SQLConsoleHTML.html"));
    } catch (IOException e) {
      LOG.error("Problem with read SQLConsoleHTML.html", e);
      HTML = "<html><head>"
          + "  <script language=\"javascript\" type=\"text/javascript\">"
          + "     function toBottom() {"
          + "        window.scrollTo(0, document.body.scrollHeight);"
          + "     }"
          + "  </script>"
          + "</head><body onLoad=\"toBottom()\">%s</body></html>";
    }
  }

  private TextEditor textEditor = new TextEditor();
  private WebView webView = new WebView();
  private StringBuffer consoleMessages = new StringBuffer();
  private NodeShower nodeShower;
  private ObjectProperty<Boolean> stopOnFirstError = new SimpleObjectProperty<>(Boolean.TRUE);
  public boolean isStopOnFirstError() { return  stopOnFirstError.getValue(); }
  @SuppressWarnings("unused")
  public void setStopOnFirstError(boolean stopOnFirstError) { this.stopOnFirstError.setValue(stopOnFirstError); }
  @SuppressWarnings("unused")
  public ObjectProperty<Boolean> stopOnFirstErrorProperty() { return stopOnFirstError; }

  /** Return text which is in code Area */
  public String getText() { return textEditor.getText(); }
  /** Set text to editor */
  public void setText(String text) { textEditor.changeText(text); }

  /** File which was read or saved */
  private File lastFile; public File lastFile() { return lastFile; }

  /** Return text which will be executed.
   * @param onCaretPosition return executed text on caret position */
  public String[] getExecutedText(boolean onCaretPosition) {
    CodeArea codeArea = textEditor.getCodeArea();
    String text = textEditor.getSelectedText();
    if (text == null || "".equals(text)) {
      text = textEditor.getText();
      if (onCaretPosition) {
        int caretPosition = codeArea.caretPositionProperty().getValue();
        String bf = "", af = "";
        if (caretPosition > 0) {
          bf = text.substring(0, caretPosition);
        }
        if (caretPosition < text.length()) {
          af = text.substring(caretPosition, text.length());
        }
        String[] bfs = bf.split("\n");
        String[] afs = af.split("\n");
        StringBuilder sb = new StringBuilder();
        boolean rowStart = caretPosition == 0 || bf.charAt(bf.length() - 1) == '\n';

        boolean skipBf = caretPosition == 0;
        if (!skipBf && rowStart) {
          int prevRecord = bf.substring(0, bf.length() - 1).lastIndexOf("\n");
          if (prevRecord >= 0) {
            skipBf = StringUtils.isBlank(bf.substring(prevRecord, bf.length() - 1));
          }
        }
        if (!skipBf) {
          for (int i = bfs.length - 1; i >= 0 && StringUtils.isNoneBlank(bfs[i]); i--) {
            if (i == bfs.length - 1 && !rowStart) {
              sb.insert(0, bfs[i]);
            } else {
              sb.insert(0, bfs[i] + "\n");
            }
          }
        }
        for (int i = 0; i < afs.length && StringUtils.isNoneBlank(afs[i]); i++) {
          sb.append(afs[i]).append("\n");
        }
        text = sb.toString();
      }
    }
    return SQLSExecutor.splitSQLS(text);
  }

  private AnchorPane node = new AnchorPane();

  /** Return controlled node */
  public Node getNode() {
    return node;
  }

  @SuppressWarnings("unchecked")
  public SQLEditorController(Consumer<Object> menuItemConsumer, Scene scene,
                             ObjectProperty<DbConfig> dbConfigProperty,
                             NodeShower nodeShower) {
    node.setMaxHeight(Double.MAX_VALUE);
    node.setMaxHeight(Double.MAX_VALUE);

    this.nodeShower = nodeShower;

    textEditor.setScene(scene);
    textEditor.changeHighlighter(new HighlighterSQL());

    nodeShower.addNode(webView, msgConsoleTitle, false);

    CodeArea ca = textEditor.createCodeArea();
    ca.setMaxHeight(Double.MAX_VALUE);
    ca.setMaxWidth(Double.MAX_VALUE);
    AnchorPane.setTopAnchor(ca, 0.0);
    AnchorPane.setBottomAnchor(ca, 0.0);
    AnchorPane.setLeftAnchor(ca, 0.0);
    AnchorPane.setRightAnchor(ca, 0.0);
    node.getChildren().add(ca);

    menuItemConsumer.accept(new SQLRunHandler(dbConfigProperty, this, handler -> nodeShower.focusNode(webView)));
    menuItemConsumer.accept(new SQLRunAllHandler(dbConfigProperty, this, handler -> nodeShower.focusNode(webView)));
    menuItemConsumer.accept(new OpenFileHandler(this));
    menuItemConsumer.accept(new SaveFileHandler(this));
    menuItemConsumer.accept(new SaveAsFileHandler(this));
    menuItemConsumer.accept(new StopOnFirstErrorOptions(stopOnFirstError));
  }

  /** Load data SQL file */
  public void loadFromFile(File file) {
    try (FileReader fr = new FileReader(file)) {
      setText(IOUtils.toString(fr));
      this.lastFile = file;
    } catch (IOException e) {
      LOG.error("The file isn't openable", e);
      ExceptionMessageFrmController.showException("The file isn't openable", e);
    }
  }

  /** Save data to SQL file */
  public void saveToFile(File file) {
    try (FileWriter fr = new FileWriter(file)) {
      IOUtils.write(getText(), fr);
      this.lastFile = file;
    } catch (IOException e) {
      LOG.error("The file isn't writable", e);
      ExceptionMessageFrmController.showException("The file isn't writable", e);
    }
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
      Platform.runLater(() -> webView.getEngine().loadContent(String.format(HTML, consoleMessages.toString())));
    } else {
      DataTableFrmController dataTableController = new DataTableFrmController(result);
      String title = result.getSql() == null ? "" : (result.getSql().length() > 50 ? result.getSql().substring(1, 45) + "..." : result.getSql());
      Platform.runLater(() -> nodeShower.addNode(dataTableController.getTabView(), title, true));
    }
  }

  /** Stop calculate highlight */
  public void stop() { textEditor.stop(); }
}
