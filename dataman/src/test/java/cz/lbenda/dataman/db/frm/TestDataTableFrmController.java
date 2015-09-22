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

import cz.lbenda.dataman.db.SQLQueryRows;
import cz.lbenda.dataman.db.TableDesc;
import cz.lbenda.test.JavaFXInitializer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 20.9.15. */
public class TestDataTableFrmController {

  @BeforeClass
  public void setup() throws InterruptedException {
    JavaFXInitializer.initialize();
  }

  @Test
  public void contstructor() {
    TableDesc td = mock(TableDesc.class);
    BooleanProperty dirty = new SimpleBooleanProperty(false);
    SQLQueryRows sqlRows = new SQLQueryRows();
    when(td.dirtyProperty()).thenReturn(dirty);
    when(td.getQueryRow()).thenReturn(sqlRows);
    when(td.getRows()).thenReturn(sqlRows.getRows());
    when(td.isLoaded()).thenReturn(false);

    // TableDesc td = new TableDesc("catalog", "schema", "TABLE", "table1");
    new DataTableFrmController(td);
    verify(td, times(1)).isLoaded();
    verify(td, times(1)).reloadRowsAction();
    when(td.isLoaded()).thenReturn(true);

    new DataTableFrmController(td);
    verify(td, times(2)).isLoaded();
    verify(td, times(1)).reloadRowsAction();
  }
}
