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

import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.PlainTextChange;
import org.fxmisc.richtext.StyleSpans;
import org.reactfx.EventStream;
import org.reactfx.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 6.9.15.
 * Class which beforeOpenInit text editor for */
public class TextEditor {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(TextEditor.class);

  private CodeArea codeArea;
  private ExecutorService executor;
  private Highlighter highlighter; public Highlighter getHighlighter() {
    if (highlighter == null) { changeHighlighter(new HighlighterPlain()); }
    return highlighter;
  }
  private Scene scene; public void setScene(Scene scene) { this.scene = scene; }
  public Scene getScene() {
    return scene == null ? codeArea != null ? codeArea.getScene() : null : scene;
  }

  /** Change text in editor */
  public void changeText(String text) {
    if (text == null) { codeArea.replaceText(0, 0, ""); }
    else { codeArea.replaceText(0, 0, text); }
  }

  /** Return text which is in code Area */
  public String getText() { return codeArea.getText(); }
  /** Return selected Text */
  public String getSelectedText() { return codeArea.getSelectedText(); }

  public void changeHighlighter(Highlighter highlighter) {
    this.highlighter = highlighter;
    getScene().getStylesheets().clear();
    if (highlighter != null) { getScene().getStylesheets().add(highlighter.stylesheetPath()); }
    if (codeArea != null) {
      computeHighlightingAsync().setOnSucceeded(event -> {
        //noinspection unchecked
        applyHighlighting(((Task<StyleSpans<Collection<String>>>) event.getSource()).getValue());
      });
    }
  }

  @SuppressWarnings("unused")
  public Scene createScene() {
    createCodeArea();
    scene = new Scene(new StackPane(codeArea), 600, 400);
    scene.getStylesheets().add(getHighlighter().stylesheetPath());
    return scene;
  }

  public CodeArea createCodeArea() {
    executor = Executors.newSingleThreadExecutor();
    codeArea = new CodeArea();
    codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
    EventStream<PlainTextChange> textChanges = codeArea.plainTextChanges();
    textChanges
        .successionEnds(Duration.ofMillis(500))
        .supplyTask(this::computeHighlightingAsync)
        .awaitLatest(textChanges)
        .map(Try::get)
        .subscribe(this::applyHighlighting);
    scene = codeArea.getScene();
    return codeArea;
  }

  private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
    String text = codeArea.getText();
    Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
      @Override
      protected StyleSpans<Collection<String>> call() throws Exception {
        return getHighlighter().computeHighlighting(text);
      }
    };
    executor.execute(task);
    return task;
  }

  private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
    codeArea.setStyleSpans(0, highlighting);
  }

  /** Stop calculate highlight */
  public void stop() {
    executor.shutdown();
  }
}
