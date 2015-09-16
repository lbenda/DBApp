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
package cz.lbenda.dataman.db;

import javafx.util.StringConverter;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 14.9.15.
 * Convert combo box item from string and to string */
public class ComboBoxItemStringConverter extends StringConverter<ComboBoxTDExtension.ComboBoxItem> {
  private ComboBoxTDExtension comboBoxTDExtension;
  public ComboBoxItemStringConverter(ComboBoxTDExtension comboBoxTDExtension) {
    this.comboBoxTDExtension = comboBoxTDExtension;
  }
  @Override
  public String toString(ComboBoxTDExtension.ComboBoxItem comboBoxItem) {
    if (comboBoxItem == null) { return null; }
    return comboBoxItem.getChoice();
  }

  @Override
  public ComboBoxTDExtension.ComboBoxItem fromString(String s) {
    return comboBoxTDExtension.itemForChoice(s);
  }
}
