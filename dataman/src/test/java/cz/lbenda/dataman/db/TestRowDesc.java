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

import static org.testng.Assert.*;

import cz.lbenda.dataman.db.dialect.HSQLDBDialect;
import org.testng.annotations.Test;

import java.sql.Types;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 20.9.15.
 * Test RowDesc implementation */
public class TestRowDesc {

  @Test
  public void stateChange() {
    RowDesc row = new RowDesc("id", new Object[] { "value1", 15, Boolean.TRUE }, RowDesc.RowDescState.LOADED);
    CatalogDesc catalogDesc = new CatalogDesc("CATALOG");
    SchemaDesc schemaDesc = new SchemaDesc(catalogDesc, "SCHEMA");
    catalogDesc.getSchemas().add(schemaDesc);
    TableDesc td = new TableDesc(schemaDesc, "TABLE", "table1");
    ColumnDesc cd = new ColumnDesc(td, "col1", null, Types.VARCHAR, "", 12, true, false, false, new HSQLDBDialect());
    cd.setPosition(0);
    ColumnDesc cd1 = new ColumnDesc(td, "col2", null, Types.INTEGER, "", 12, true, false, false, new HSQLDBDialect());
    cd1.setPosition(1);

    String originalValue = row.getColumnValue(cd);
    Integer originalValue1 = row.getColumnValue(cd1);
    row.setColumnValue(cd, "Jina hodnota");
    row.setColumnValue(cd1, 8);
    assertEquals(row.getState(), RowDesc.RowDescState.CHANGED);
    row.setColumnValue(cd, originalValue);
    assertEquals(row.getState(), RowDesc.RowDescState.CHANGED);
    row.setColumnValue(cd1, originalValue1);
    assertEquals(row.getState(), RowDesc.RowDescState.LOADED);
  }
}
