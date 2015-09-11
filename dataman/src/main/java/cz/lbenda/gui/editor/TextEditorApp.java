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

import cz.lbenda.dbapp.rc.SessionConfiguration;
import cz.lbenda.dbapp.rc.db.DbStructureReader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 6.9.15.
 * Stand allone application for text editor */
public class TextEditorApp extends Application {

  private static final String sampleCode = String.join("\n", new String[]{
      "select * from dataman.hobiman;"
  });

  public static final void main(String[] args) {
    launch(args);
  }

  SQLEditor te;

  @Override
  public void start(Stage primaryStage) throws Exception {
    SessionConfiguration sessionConfiguration = new SessionConfiguration();
    sessionConfiguration.load(this.getClass().getResource("dbApp.dtm"));
    DbStructureReader sqlExecutor = new DbStructureReader(sessionConfiguration);
    te = new SQLEditor(sqlExecutor);
    Scene scene = te.createScene();
    // te.changeHighlighter(new HighlighterSQL());
    primaryStage.setScene(scene);
    primaryStage.setTitle("Text editor");
    primaryStage.show();
    primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("sqlEditor.png")));
    primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("sqlEditor24.png")));
    primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("sqlEditor32.png")));

    // te.getWebView().getEngine().loadContent("<html><head></head><body><h1>Hello world</h1></body></html>");
    // te.getWebView().getEngine().load("http://www.facebook.com");
  }

  @Override
  public void stop() {
    te.stop();
  }
}
