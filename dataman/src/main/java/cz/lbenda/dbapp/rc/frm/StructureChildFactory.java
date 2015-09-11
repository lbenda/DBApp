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
package cz.lbenda.dbapp.rc.frm;

import cz.lbenda.dbapp.actions.*;
import cz.lbenda.dbapp.rc.SessionConfiguration;
import cz.lbenda.dbapp.rc.db.TableDescription;
import java.beans.IntrospectionException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.openide.actions.CustomizeAction;
import org.openide.actions.OpenAction;
import org.openide.awt.Actions;
import org.openide.cookies.OpenCookie;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.BeanNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Factory for FrmDbStructureTopComponent tree
* Created by Lukas Benda <lbenda @ lbenda.cz> on 9/28/14.
*/
@Messages({
    "ERR_ConnectionNotEstablished=The connection isn't established: %s",
    "TITLE_SaveConfiguration=Choose file for save configuration",
    "TITLE_LoadConfiguration=Choose file for which is configuration load",
    "FILES_XMLandDTM=XML & DTM files",
    "TITLE_RemoveConfiguration_YesNoDialog=Warning!!!",
    "MSG_RemoveConfiguration_YesNoDialog=Would you remove configuration: %s"
})
public class StructureChildFactory extends ChildFactory<Object> {

  private static final Logger LOG = LoggerFactory.getLogger(StructureChildFactory.class);

  private final StrucHolder strucHolder;

  public StructureChildFactory(StrucHolder strucHolder) {
    this.strucHolder = strucHolder;
  }
  private void createSCKeys(final List<Object> toPopulate) {
    SessionConfiguration.getConfigurations().forEach((SessionConfiguration sc) ->
      toPopulate.add(new StrucHolder(sc, null, null, null, StrucLevel.SC)));
  }
  private boolean createCatalogKeys(SessionConfiguration sc, List<Object> toPopulate) {
    try {
      sc.reloadStructure();
    } catch (Exception e) {
      LOG.error("Problem with reload structure", e);
      toPopulate.add(new ErrorNode(String.format(Bundle.ERR_ConnectionNotEstablished(), e), this));
      return true;
    }
    sc.getCatalogs().forEach((String cat) -> {
      if (sc.isShowCatalog(cat)) { toPopulate.add(new StrucHolder(sc, cat, null, null, StrucLevel.SCHEMA)); }
    });
    if (toPopulate.isEmpty()) {
      for (String cat : sc.getCatalogs()) {
        createSchemaKeys(sc, cat, toPopulate);
      }
    }
    return true;
  }
  private void createSchemaKeys(SessionConfiguration sc, String catalog, List<Object> toPopulate) {
    sc.getSchemas(catalog).forEach((String sch) -> {
      if (sc.isShowSchema(catalog, sch)) { toPopulate.add(new StrucHolder(sc, catalog, sch, null, StrucLevel.TABLE_TYPE)); }
    });
    if (toPopulate.isEmpty()) {
      sc.getSchemas(catalog).forEach((String sch1) -> createTableTypeKeys(sc, catalog, sch1, toPopulate));
    }
  }
  private void createTableTypeKeys(SessionConfiguration sc, String catalog, String schema, List<Object> toPopulate) {
    sc.shownTableType(catalog, schema).forEach((TableDescription.TableType tt) ->
      toPopulate.add(new StrucHolder(sc, catalog, schema, tt, StrucLevel.TABLE)));
  }
  protected void createTDKeys(SessionConfiguration sc, String catalog, String schema,
                              TableDescription.TableType tableType, List<Object> toPopulate) {
    List<TableDescription> tds = sc.getTableDescriptions(catalog, schema, tableType);
    Collections.sort(tds);
    toPopulate.addAll(tds);
  }

  @Override
  protected boolean createKeys(List<Object> toPopulate) {
    switch (strucHolder.getLevel()) {
      case SC : createSCKeys(toPopulate); break;
      case CATALOG : return createCatalogKeys(strucHolder.getSessionConfiguration(), toPopulate);
      case SCHEMA : createSchemaKeys(strucHolder.getSessionConfiguration(), strucHolder.getCatalog(), toPopulate); break;
      case TABLE_TYPE : createTableTypeKeys(strucHolder.getSessionConfiguration(), strucHolder.getCatalog(), strucHolder.getSchema(), toPopulate); break;
      case TABLE : createTDKeys(strucHolder.getSessionConfiguration(), strucHolder.getCatalog(), strucHolder.getSchema(), strucHolder.getTableType(), toPopulate); break;
    }
    return true;
  }
  @Override
  protected Node createNodeForKey(Object key) {
    if (key instanceof StrucHolder) {
      StrucHolder sh = (StrucHolder) key;
      if (StrucLevel.SC.equals(sh.getLevel())) {
        try {
          return new SessionConfigurationNode(sh.getSessionConfiguration());
        } catch (IntrospectionException ex) {
          LOG.error("Failed node create", ex);
          Exceptions.printStackTrace(ex);
        }
      } else {
        return new StructureNode(sh);
      }
    } else if (key instanceof ErrorNode) {
      return (ErrorNode) key;
    } else {
      try {
        return new TableDescriptionNode((TableDescription) key);
      } catch (IntrospectionException ex) {
        LOG.error("Failed node create", ex);
        Exceptions.printStackTrace(ex);
      }
    }
    return null;
  }

  public enum StrucLevel {
    SC, CATALOG, SCHEMA, TABLE_TYPE, TABLE, ;
  }

  public static class StrucHolder {
    private final SessionConfiguration sessionConfiguration; public SessionConfiguration getSessionConfiguration() { return sessionConfiguration; }
    private final String catalog; public String getCatalog() { return catalog; }
    private final String schema; public String getSchema() { return schema; }
    private final TableDescription.TableType tableType; public TableDescription.TableType getTableType() { return tableType; }
    private final StrucLevel level; public StrucLevel getLevel() { return level; }
    public StrucHolder(SessionConfiguration sessionConfiguration, String catalog, String schema,
                       TableDescription.TableType tableType, StrucLevel level) {
      this.sessionConfiguration = sessionConfiguration;
      this.catalog = catalog;
      this.schema = schema;
      this.tableType = tableType;
      this.level = level;
    }
  }

  public static class RootNode extends AbstractNode {
    public RootNode() {
      this(new InstanceContent());
    }
    public RootNode(InstanceContent ic) {
      super(Children.create(new StructureChildFactory(new StrucHolder(null, null, null, null, StrucLevel.SC)), true),
          new AbstractLookup(ic));
      // super(Children.LEAF, new AbstractLookup(ic));
      ic.add((OpenCookie) () -> {throw new UnsupportedOperationException("Not supported yet.");}); //To change body of generated methods, choose Tools | Templates.
      ic.add((ImportCookie) () -> {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(Bundle.FILES_XMLandDTM(), "XML", "DTM");
        chooser.setFileFilter(filter);
        chooser.setDialogTitle(Bundle.TITLE_SaveConfiguration());
        int returnVal = chooser.showOpenDialog(WindowManager.getDefault().getMainWindow());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          SessionConfiguration sc = new SessionConfiguration();
          sc.load(chooser.getSelectedFile().getAbsoluteFile());
          SessionConfiguration.registerNewConfiguration(sc);
        }});
      setDisplayName("Sessions");
    }
    @Override
    public Action[] getActions(boolean context) {
      List<? extends Action> myActions = Utilities.actionsForPath("Actions/DBStructure/Session/Create");
      Action[] result = new Action[myActions.size() + 1];
      myActions.forEach(a -> result[myActions.indexOf(a)] = a);
      result[result.length - 1] = Actions.forID("DBStructure", "cz.lbenda.dbapp.actions.ImportAction");
      return result;
    }
  }

  public static class ErrorNode extends AbstractNode {
    public ErrorNode(String message, StructureChildFactory scf) {
      this(message, scf, new InstanceContent());
    }
    public ErrorNode(String message, final StructureChildFactory scf, InstanceContent ic) {
      super(Children.LEAF, new AbstractLookup(ic));
      setDisplayName(message);
      ic.add((ReloadCookie) () -> scf.refresh(true));
    }
    @Override
    public Action[] getActions(boolean context) {
      List<? extends Action> myActions = Utilities.actionsForPath("Actions/DBStructure");
      return myActions.toArray(new Action[myActions.size()]);
    }
  }

  public static class StructureNode extends AbstractNode {
    public StructureNode(StrucHolder sh) {
      super(Children.create(new StructureChildFactory(sh), true));
      switch (sh.getLevel()) {
        case CATALOG : setDisplayName(sh.getSessionConfiguration().getId()); break;
        case SCHEMA : setDisplayName(sh.getCatalog()); break;
        case TABLE_TYPE : setDisplayName(sh.getSchema()); break;
        case TABLE :
          setDisplayName(sh.getTableType().toString());
          switch (sh.getTableType()) {
            case TABLE : setIconBaseWithExtension("cz/lbenda/dbapp/rc/frm/table.png");
              break;
            case VIEW:
              setIconBaseWithExtension("cz/lbenda/dbapp/rc/frm/table_view.png");
              break;
          }
          break;
      }
    }
  }

  public static class SessionConfigurationNode extends BeanNode<SessionConfiguration> {
    public SessionConfigurationNode(final SessionConfiguration sc) throws IntrospectionException {
      this(sc, new InstanceContent());
    }
    public SessionConfigurationNode(final SessionConfiguration sc, final InstanceContent ic) throws IntrospectionException {
      super(sc, Children.create(new StructureChildFactory(new StrucHolder(sc, null, null, null, StrucLevel.CATALOG)), true), new AbstractLookup(ic));
      ic.add((OpenCookie) sc::reloadStructure);
      ic.add((ReloadCookie) sc::reloadStructure);
      ic.add((ExportCookie) () -> {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(Bundle.FILES_XMLandDTM(), "XML", "DTM");
        chooser.setFileFilter(filter);
        chooser.setDialogTitle(Bundle.TITLE_SaveConfiguration());
        int returnVal = chooser.showSaveDialog(WindowManager.getDefault().getMainWindow());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          sc.saveToFile(chooser.getSelectedFile().getAbsoluteFile());
        }
      });
      ic.add((RemoveCookie) () -> {
        int choose = JOptionPane.showConfirmDialog(null,
            String.format(Bundle.MSG_RemoveConfiguration_YesNoDialog(), sc.getId()),
            Bundle.TITLE_RemoveConfiguration_YesNoDialog(), JOptionPane.YES_NO_OPTION);
        if (choose == JOptionPane.OK_OPTION) {
          SessionConfiguration.removeConfiguration(sc);
        }
      });
      ic.add((CloseCookie) sc::close);
      setDisplayName(sc.getId());
      setIconBaseWithExtension("cz/lbenda/dbapp/rc/frm/database.png");
    }
    @Override
    public Action getPreferredAction() {
      return SystemAction.get(CustomizeAction.class);
    }
    @Override
    public Action[] getActions(boolean context) {
      return new Action[]{SystemAction.get(CustomizeAction.class),
          Actions.forID("DBStructure", "cz.lbenda.dbapp.actions.ReloadAction"),
          Actions.forID("DBStructure", "cz.lbenda.dbapp.actions.ExportAction"),
          Actions.forID("DBStructure", "cz.lbenda.dbapp.actions.RemoveAction"),
          Actions.forID("DBStructure", "cz.lbenda.dbapp.actions.CloseAction")
      };
    }
  }

  public static class TableDescriptionNode extends BeanNode<TableDescription> {
    public TableDescriptionNode(final TableDescription td) throws IntrospectionException {
      this(td, new InstanceContent());
    }

    public TableDescriptionNode(final TableDescription td, final InstanceContent ic) throws IntrospectionException {
      super(td, Children.LEAF, new AbstractLookup(ic));
      ic.add((OpenCookie) () -> {
        TopComponent tc = findTheTableComponent(td);
        if (tc == null) {
          tc = new FrmDbTableTopComponent(td);
          tc.open();
        }
        tc.requestActive();
      });
      setDisplayName(td.getName());
      setShortDescription(td.getComment());
      switch (td.getTableType()) {
        case TABLE : setIconBaseWithExtension("cz/lbenda/dbapp/rc/frm/table.png"); break;
        case VIEW : setIconBaseWithExtension("cz/lbenda/dbapp/rc/frm/table_view.png"); break;
      }
    }

    private TopComponent findTheTableComponent(TableDescription td) {
      Set<TopComponent> openTC = WindowManager.getDefault().getRegistry().getOpened();
      for (TopComponent tc : openTC) {
        if (tc.getLookup().lookup(TableDescription.class) == td) {
          return tc;
        }
      }
      return null;
    }

    @Override
    public Action getPreferredAction() {
      return SystemAction.get(OpenAction.class);
    }

    @Override
    public Action[] getActions(boolean context) {
      return new Action[]{SystemAction.get(OpenAction.class)};
    }
  }
}
