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

import cz.lbenda.schema.dbapp.exconf.*;
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

    public static void loadExtensions(SessionConfiguration sc, final TableDescriptionExtensionsType tableDescriptionExtensions) {
      if (tableDescriptionExtensions == null || tableDescriptionExtensions.getTableDescriptionExtension().isEmpty()) {
        LOG.debug("No table extensions defined");
        return;
      }
      for (TableDescriptionExtensionType tdExtension : tableDescriptionExtensions.getTableDescriptionExtension()) {
        String catalog = tdExtension.getCatalog();
        String schema = tdExtension.getSchema();
        String table = tdExtension.getTable();
        TableDescription td = sc.getOrCreateTableDescription(catalog, schema, table);

        if (tdExtension.getComboBox() == null || tdExtension.getComboBox().isEmpty()) {
          LOG.debug("No combo box defined on table");
        } else {
          for (ComboBoxType comboBox : tdExtension.getComboBox()) {
            loadComboBox(sc, td, comboBox);
          }
        }
      }
    }

    private static void loadComboBox(SessionConfiguration sc, TableDescription td, ComboBoxType comboBox) {
      ComboBoxTDExtension sb = new ComboBoxTDExtension(td, comboBox.getColumn(),
          ((TableOfKeySQLType) comboBox.getTableOfKeySQL()).getId());
      sb.setColumnValue(comboBox.getColumnValue());
      sb.setColumnChoice(comboBox.getColumnChoice());
      sb.setColumnTooltip(comboBox.getColumnTooltip());
      if (comboBox.getReloadOn() == null || comboBox.getReloadOn().isEmpty()) {
        LOG.debug("No reload defined on combo box");
      } else {
        for (DbTableType reloadOn : comboBox.getReloadOn()) {
          TableDescription td1 = sc.getOrCreateTableDescription(reloadOn.getCatalog(), reloadOn.getSchema(), reloadOn.getTable());
          td1.getReloadableExtension().add(sb);
        }
      }
      td.getExtensions().add(sb);
    }
  }
}
