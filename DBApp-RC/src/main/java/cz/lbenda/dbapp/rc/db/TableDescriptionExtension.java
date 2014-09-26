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
package cz.lbenda.dbapp.rc.db;

import cz.lbenda.dbapp.rc.SessionConfiguration;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** General interface for all table description extension.
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/16/14.
 */
public interface TableDescriptionExtension {

  enum TableAction {
    SELECT, UPDATE, DELETE, INSERT, ;
  }

  /** Return table description which this class extends */
  TableDescription getTableDescription();

  /** Inform the extension when table was changed
   * @param td table which is affected
   * @param action action which was executed at the table
   */
  void tableWasChanged(TableDescription td, TableAction action);

  /** Return element to which is this extension stored */
  Element storeToElement();

  /** Columns which is extended by table extension
   * @return columns which is extended. If extension isn't for column then is empty list returned
   */
  List<Column> getColumns();

  abstract class XMLReaderWriterHelper {
    private static final Logger LOG = LoggerFactory.getLogger(XMLReaderWriterHelper.class);

    public static void loadExtensions(SessionConfiguration sc, final Element element) {
      LOG.trace("load table dsc. extension");
      if (element == null) { return; }
      for (Element tdExtension : element.getChildren("tableDescriptionExtension")) {
        String catalog = tdExtension.getAttributeValue("catalog");
        String schema = tdExtension.getAttributeValue("schema");
        String table = tdExtension.getAttributeValue("table");
        TableDescription td = sc.getOrCreateTableDescription(catalog, schema, table);

        for (Element select : tdExtension.getChildren("comboBox")) { loadComboBox(sc, td, select); }
      }
    }

    private static void loadComboBox(SessionConfiguration sc, TableDescription td, Element element) {
      LOG.trace("load combo box");
      ComboBoxTDExtension sb = new ComboBoxTDExtension(td, element.getAttributeValue("column"),
          element.getAttributeValue("tableOfKeySQL"));
      sb.setColumnValue(element.getAttributeValue("column_value"));
      sb.setColumnChoice(element.getAttributeValue("column_choice"));
      sb.setColumnTooltip(element.getAttributeValue("column_tooltip"));
      for (Element reloadOn : element.getChildren("reloadOn")) {
        TableDescription td1 = sc.getOrCreateTableDescription(reloadOn.getAttributeValue("catalog"),
            reloadOn.getAttributeValue("schema"), reloadOn.getAttributeValue("table"));
        td1.getReloadableExtension().add(sb);
      }
      td.getExtensions().add(sb);
    }
  }
}
