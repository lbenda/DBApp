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

import org.fxmisc.richtext.StyleSpans;
import org.fxmisc.richtext.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 6.9.15.
 * Plain text - nothing to do */
public class HighlighterPlain implements Highlighter {

  @Override
  public StyleSpans<Collection<String>> computeHighlighting(String text) {
    StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
    spansBuilder.add(Collections.emptyList(), text.length());
    return spansBuilder.create();
  }

  @Override
  public String stylesheetPath() {
    return HighlighterJava.class.getResource("plain-highlighting.css").toExternalForm();
  }
}
