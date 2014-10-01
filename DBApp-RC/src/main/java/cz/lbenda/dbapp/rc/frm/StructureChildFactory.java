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

import cz.lbenda.dbapp.rc.SessionConfiguration;
import cz.lbenda.dbapp.rc.db.TableDescription;
import java.beans.IntrospectionException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.Action;
import org.openide.actions.OpenAction;
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

/**
* Created by Lukas Benda <lbenda @ lbenda.cz> on 9/28/14.
*/
@Messages({
    "ERR_ConnectionNotEstablished=The connection isn't established: %s"
})
public class StructureChildFactory extends ChildFactory<Object> {

  private static final Logger LOG = LoggerFactory.getLogger(StructureChildFactory.class);

  private final StrucHolder strucHolder;

  public StructureChildFactory(StrucHolder strucHolder) {
    this.strucHolder = strucHolder;
  }
  private void createSCKeys(List<Object> toPopulate) {
    for (SessionConfiguration sc : SessionConfiguration.getConfigurations()) {
      toPopulate.add(new StrucHolder(sc, null, null, null));
    }
  }
  private boolean createCatalogKeys(SessionConfiguration sc, List<Object> toPopulate) {
    try {
      sc.reloadStructure();
    } catch (Exception e) {
      LOG.error("Problem with reload structure", e);
      toPopulate.add(new ErrorNode(String.format(Bundle.ERR_ConnectionNotEstablished(), e), this));
      return true;
    }
    for (String cat : sc.getCatalogs()) {
      if (sc.isShowCatalog(cat)) { toPopulate.add(new StrucHolder(sc, cat, null, null)); }
    }
    if (toPopulate.isEmpty()) {
      for (String cat : sc.getCatalogs()) {
        createSchemaKeys(sc, cat, toPopulate);
      }
    }
    return true;
  }
  private void createSchemaKeys(SessionConfiguration sc, String catalog, List<Object> toPopulate) {
    for (String sch : sc.getSchemas(catalog)) {
      if (sc.isShowSchema(catalog, sch)) { toPopulate.add(new StrucHolder(sc, catalog, sch, null)); }
    }
    if (toPopulate.isEmpty()) {
      for (String sch : sc.getSchemas(catalog)) {
        createTableTypeKeys(sc, catalog, sch, toPopulate);
      }
    }
  }
  private void createTableTypeKeys(SessionConfiguration sc, String catalog, String schema, List<Object> toPopulate) {
    for (TableDescription.TableType tt : sc.shownTableType(catalog, schema)) {
      toPopulate.add(new StrucHolder(sc, catalog, schema, tt, 4));
    }
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
      case 0 : createSCKeys(toPopulate); break;
      case 1 : return createCatalogKeys(strucHolder.getSessionConfiguration(), toPopulate);
      case 2 : createSchemaKeys(strucHolder.getSessionConfiguration(), strucHolder.getCatalog(), toPopulate); break;
      case 3 : createTableTypeKeys(strucHolder.getSessionConfiguration(), strucHolder.getCatalog(), strucHolder.getSchema(), toPopulate); break;
      case 4 : createTDKeys(strucHolder.getSessionConfiguration(), strucHolder.getCatalog(), strucHolder.getSchema(), strucHolder.getTableType(), toPopulate); break;
    }
    return true;
  }
  @Override
  protected Node createNodeForKey(Object key) {
    if (key instanceof StrucHolder) {
      StrucHolder sh = (StrucHolder) key;
      if (sh.getLevel() == 0) {
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

  public static class StrucHolder {
    private final SessionConfiguration sessionConfiguration; public SessionConfiguration getSessionConfiguration() { return sessionConfiguration; }
    private final String catalog; public String getCatalog() { return catalog; }
    private final String schema; public String getSchema() { return schema; }
    private final TableDescription.TableType tableType; public TableDescription.TableType getTableType() { return tableType; }
    private final int level; public int getLevel() { return level; }
    public StrucHolder(SessionConfiguration sessionConfiguration, String catalog, String schema, TableDescription.TableType tableType) {
      this.sessionConfiguration = sessionConfiguration;
      this.catalog = catalog;
      this.schema = schema;
      this.tableType = tableType;
      level = tableType == null ? schema == null ? catalog == null ? sessionConfiguration == null ? 0 : 1 : 2 : 3 : 4;
    }
    public StrucHolder(SessionConfiguration sessionConfiguration, String catalog, String schema,
                       TableDescription.TableType tableType, int level) {
      this.sessionConfiguration = sessionConfiguration;
      this.catalog = catalog;
      this.schema = schema;
      this.tableType = tableType;
      this.level = level;
    }
  }

  public static class RootNode extends AbstractNode {
    public RootNode() {
      super(Children.create(new StructureChildFactory(new StrucHolder(null, null, null, null)), true));
      setDisplayName("Sessions");
    }
  }

  public static class ErrorNode extends AbstractNode {
    public ErrorNode(String message, StructureChildFactory scf) {
      this(message, scf, new InstanceContent());
    }
    public ErrorNode(String message, final StructureChildFactory scf, InstanceContent ic) {
      super(Children.LEAF, new AbstractLookup(ic));
      setDisplayName(message);
      ic.add(new ReloadCookie() { @Override public void reload() { scf.refresh(true); }});
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
        case 1 : setDisplayName(sh.getSessionConfiguration().getId()); break;
        case 2 : setDisplayName(sh.getCatalog()); break;
        case 3 : setDisplayName(sh.getSchema()); break;
        case 4 :
          setDisplayName(sh.getTableType().toString());
          switch (sh.getTableType()) {
            case TABLE : setIconBaseWithExtension("cz/lbenda/dbapp/rc/frm/table.png"); break;
            case VIEW : setIconBaseWithExtension("cz/lbenda/dbapp/rc/frm/table_view.png"); break;
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
      super(sc, Children.create(new StructureChildFactory(new StrucHolder(sc, null, null, null)), true), new AbstractLookup(ic));
      ic.add(new OpenCookie() {
        @Override
        public void open() { sc.reloadStructure(); }
      });
      setDisplayName(sc.getId());
      setIconBaseWithExtension("cz/lbenda/dbapp/rc/frm/database.png");
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

  public static class TableDescriptionNode extends BeanNode<TableDescription> {
    public TableDescriptionNode(final TableDescription td) throws IntrospectionException {
      this(td, new InstanceContent());
    }

    public TableDescriptionNode(final TableDescription td, final InstanceContent ic) throws IntrospectionException {
      super(td, Children.LEAF, new AbstractLookup(ic));
      ic.add(new OpenCookie() {
        @Override
        public void open() {
          TopComponent tc = findTheTableComponent(td);
          if (tc == null) {
            // tc = new FrmTableGridTopComponent(td);
            tc = new FrmDbTableTopComponent(td);
            tc.open();
          }
          tc.requestActive();
        }
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
