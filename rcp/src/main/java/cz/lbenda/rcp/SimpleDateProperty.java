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
package cz.lbenda.rcp;

import javafx.beans.property.SimpleObjectProperty;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 13.9.15. */
public class SimpleDateProperty extends SimpleObjectProperty<Date> {
  public SimpleDateProperty() { super(); }
  public SimpleDateProperty(Date var1) { super(var1); }
  public SimpleDateProperty(Object var1, String var2) { super(var1, var2); }
  public SimpleDateProperty(Object var1, String var2, Date var3) { super(var1, var2, var3); }

   public void setLocalDate(LocalDate localDate) {
    setValue(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
  }
  public LocalDate getLocalDate() {
    return getValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }
}
