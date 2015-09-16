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

import cz.lbenda.dataman.rc.DbConfig;
import java.util.List;

import cz.lbenda.dataman.schema.exconf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** General interface for all table description extension.
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 9/16/14.
 */
public interface TableDescriptionExtension {

  /** Audit type which switch of auditing for table */
  AuditType NONE_AUDIT = new AuditType();

  enum TableAction {
    SELECT, UPDATE, DELETE, INSERT, ;
  }

  /** Return table description which this class extends */
  TableDesc getTableDescription();

  /** Inform the extension when table was changed
   * @param td table which is affected
   * @param action action which was executed at the table
   */
  void tableWasChanged(TableDesc td, TableAction action);

  /** Columns which is extended by table extension
   * @return columns which is extended. If extension isn't for column then is empty list returned
   */
  List<ColumnDesc> getColumns();

  abstract class XMLReaderWriterHelper {
    private static final Logger LOG = LoggerFactory.getLogger(XMLReaderWriterHelper.class);
    static {
      NONE_AUDIT.setType(AuditTypeType.NONE);
    }

    public static void loadExtensions(DbConfig sc, final ExConfType exConf) {
      if (exConf == null || exConf.getTables() == null || exConf.getTables().getTable().isEmpty()) {
        LOG.debug("No table extensions defined");
        return;
      }
      for (TableType tdExtension : exConf.getTables().getTable()) {
        String catalog = tdExtension.getCatalog();
        String schema = tdExtension.getSchema();
        String table = tdExtension.getTable();
        TableDesc td = sc.getOrCreateTableDescription(catalog, schema, table);
        td.setAudit(auditTypeForTable(exConf, td));

        if (tdExtension.getComboBox() == null || tdExtension.getComboBox().isEmpty()) {
          LOG.debug("No combo box defined on table");
        } else {
          for (ComboBoxType comboBox : tdExtension.getComboBox()) {
            loadComboBox(sc, td, comboBox);
          }
        }
      }
    }

    /** Return audit type for given table description. If no configuration is set then return audit type NONE, elsewhere
     * return configuration from table, and if not exist then from schema.
     * @param tableDescription description of table
     * @return audit type, which is configure for given table. Never return null.
     */
    public static AuditType auditTypeForTable(ExConfType exConf, TableDesc tableDescription) {
      if (exConf == null) { return NONE_AUDIT; }
      if (exConf.getTables() != null) {
        for (TableType table : exConf.getTables().getTable()) {
          if (tableDescription.getCatalog().equals(table.getCatalog())
              && tableDescription.getSchema().equals(table.getSchema())
              && tableDescription.getName().equals(table.getTable())
              && table.getAudit() != null) {
            return table.getAudit();
          }
        }
      }
      if (exConf.getSchemas() != null) {
        for (SchemaType schema : exConf.getSchemas().getSchema()) {
          if (tableDescription.getCatalog().equals(schema.getCatalog())
              && tableDescription.getSchema().equals(schema.getSchema())
              && schema.getAudit() != null) { return schema.getAudit(); }
        }
      }
      return NONE_AUDIT;
    }

    private static void loadComboBox(DbConfig sc, TableDesc td, ComboBoxType comboBox) {
      ComboBoxTDExtension sb = new ComboBoxTDExtension(td, comboBox.getColumn(),
          ((TableOfKeySQLType) comboBox.getTableOfKeySQL()).getId());
      sb.setColumnValue(comboBox.getColumnValue());
      sb.setColumnChoice(comboBox.getColumnChoice());
      sb.setColumnTooltip(comboBox.getColumnTooltip());
      if (comboBox.getReloadOn() == null || comboBox.getReloadOn().isEmpty()) {
        LOG.debug("No reload defined on combo box");
      } else {
        for (DbTableType reloadOn : comboBox.getReloadOn()) {
          TableDesc td1 = sc.getOrCreateTableDescription(reloadOn.getCatalog(), reloadOn.getSchema(), reloadOn.getTable());
          td1.getReloadableExtension().add(sb);
        }
      }
      td.getExtensions().add(sb);
    }
  }
}
