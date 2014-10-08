/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.rc.frm;

import cz.lbenda.dbapp.actions.DBTableCancelCookie;
import cz.lbenda.dbapp.actions.DBTableReloadCookie;
import cz.lbenda.dbapp.actions.DBTableRowAddCookie;
import cz.lbenda.dbapp.actions.DBTableRowsRemoveCookie;
import cz.lbenda.dbapp.rc.db.Column;
import cz.lbenda.dbapp.rc.db.TableDescription;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.CellEditor;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import cz.lbenda.dbapp.rc.frm.gui.ColumnCellRenderer;
import org.netbeans.swing.etable.ETable;
import org.netbeans.swing.etable.ETableColumn;
import org.netbeans.swing.etable.ETableColumnModel;
import org.openide.awt.UndoRedo;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.OutlineView;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.windows.TopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top component which displays something.
 */
@TopComponent.Description(
        preferredID = "FrnDbTableTopComponent",
        iconBase = "cz/lbenda/dbapp/rc/frm/table.png",
        persistenceType = TopComponent.PERSISTENCE_NEVER// TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
// @ActionID(category = "Window", id = "cz.lbenda.dbapp.rc.frm.FrnDbTableTopComponent")
// @ActionReference(path = "Menu/Window" /*, position = 333 */)
@Messages({
})
public final class FrmDbTableTopComponent extends TopComponent implements ExplorerManager.Provider {

  private static final Logger LOG = LoggerFactory.getLogger(FrmDbTableTopComponent.class);
  private final ExplorerManager em = new ExplorerManager();
  private final TableDescription td;
  private final OutlineView ov;
  private final InstanceContent ic = new InstanceContent();

  private UndoRedo.Manager manager = new UndoRedo.Manager();

  public FrmDbTableTopComponent(TableDescription td) {
    this.td = td;
    td.addUndoableEditListener(manager);
    initComponents();
    registerActionMap();
    setHtmlDisplayName(generateTitle());
    setLayout(new BorderLayout());
    ov = new OutlineView();
    ov.getOutline().setRootVisible(false);
    ov.getOutline().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    ((ETableColumnModel) ov.getOutline().getColumnModel()).setColumnHidden(((ETableColumn) ov.getOutline().getColumnModel().getColumn(0)), true);

    definePropertyAndHint();

    ColumnCellRenderer ccr = new ColumnCellRenderer(td.getColumns());
    ccr.setCentered(false);
    ov.getOutline().setDefaultRenderer(Node.Property.class, ccr);
    add(ov, BorderLayout.CENTER);
    Node rootNode = new AbstractNode(Children.create(new RowChildFactory(td), true));
    em.setRootContext(rootNode);

    ic.add(addCookie);
    ic.add(removeCookie);
    ic.add(cancelCookie);
    ic.add(reloadCookie);
    ic.add(em);
    ic.add(getActionMap());
    associateLookup(new AbstractLookup(ic));

    /*
    for (Column col : td.getColumns()) {
      CellEditor ce = ((ETable) ov.getOutline()).getCellEditor(1, col.getPosition() + 1);
    }
    */
    ov.getOutline().getSelectionModel().addListSelectionListener(listSelectionListener);
  }

  private void definePropertyAndHint() {
    String[] ar = new String[td.getColumns().size() * 2];
    int i = 0;
    for (Column column : td.getColumns()) {
      ar[i] = column.getName();
      ar[i + 1] = column.getName();
      i += 2;
    }
    ov.setPropertyColumns(ar);
    for (Column column : td.getColumns()) {
      ov.setPropertyColumnDescription(column.getName(), column.getComment());
    }
  }

  private TableModelListener tableModelListener = new TableModelListener() {
    @Override
    public void tableChanged(final TableModelEvent e) {
      if (TableModelEvent.INSERT == e.getType()) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            int row = td.getRows().size() - 1;
            ov.getOutline().setRowSelectionInterval(row, row);
            for (Column col : td.getColumns()) {
              if (!col.isAutoincrement() && !col.isGenerated()) {
                ov.getOutline().setColumnSelectionInterval(col.getPosition() + 1, col.getPosition() + 1);
                ov.getOutline().scrollRectToVisible(new Rectangle(ov.getOutline().getCellRect(row,
                        col.getPosition() + 1, true)));
                ov.getOutline().editCellAt(row, col.getPosition() + 1);
                break;
              }
            }
          }
        });
      }
    }
  };

  private ListSelectionListener listSelectionListener = new ListSelectionListener() {
    @Override
    public void valueChanged(ListSelectionEvent e) {
      if (ov.getOutline().getSelectedRow() != -1) {
        RowNode.Row row = new RowNode.Row(td, td.getRows().get(ov.getOutline().getSelectedRow()));
        ChosenTable.getDefault().setSelectedRowValues(row);
      }
    }
  };

  private DBTableRowAddCookie addCookie = new DBTableRowAddCookie() {
    @Override
    public void addRow() {
      ov.getOutline().getModel().addTableModelListener(tableModelListener);
      td.createRow();
      ov.getOutline().unsetQuickFilter();
      ic.add(td);
    }
  };

  private DBTableRowsRemoveCookie removeCookie = new DBTableRowsRemoveCookie() {
    @Override
    public void removeRows() {
      td.removeRows(ov.getOutline().getSelectedRows());
      ic.add(td);
    }
  };

  private DBTableCancelCookie cancelCookie = new DBTableCancelCookie() {
    @Override
    public void cancelChanges() {
      td.cancelChanges();
    }
  };

  private DBTableReloadCookie reloadCookie = new DBTableReloadCookie() {
    @Override
    public void reloadTable() {
      td.reloadRows();
    }
  };

  @Override
  public UndoRedo getUndoRedo() {
    return manager;
  }

  private String generateTitle() {
    StringBuilder title = new StringBuilder("<html><body><small><small>");
    title.append(td.getSessionConfiguration().getId());
    title.append("</small></small> ");
    if (td.getSessionConfiguration().isShowCatalog(td.getCatalog())) { title.append(td.getCatalog()).append("."); }
    if (td.getSessionConfiguration().isShowSchema(td.getSchema(), td.getCatalog())) { title.append(td.getSchema()).append("."); }
    title.append("<b>").append(td.getName()).append("</b></body></html>");
    return title.toString();
  }

  public void registerActionMap() {
    ActionMap am = super.getActionMap();
    List<? extends Action> myActions = Utilities.actionsForPath("Toolbars/DBTable");
    LOG.trace("Count of actions: " + myActions.size());
    for (Action a : myActions) {
      a.setEnabled(true);
      am.put("cz.lbenda.dbapp.actions.RowAddAction", a);
    }
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 400, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 300, Short.MAX_VALUE)
    );
  }// </editor-fold>//GEN-END:initComponents

  @Override
  public void componentActivated() {
    super.componentActivated();
    ChosenTable.getDefault().setTableDescription(td);
    ExplorerUtils.activateActions(em, true);
  }

  @Override
  public void componentDeactivated() {
    ExplorerUtils.activateActions(em, false);
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables
  @Override
  public void componentOpened() {
    // TODO add custom code on component opening
  }

  @Override
  public void componentClosed() {
    // TODO add custom code on component closing
  }

  @Override
  public ExplorerManager getExplorerManager() {
    return em;
  }

  public class RowChildFactory extends ChildFactory<cz.lbenda.dbapp.rc.frm.RowNode.Row> {
    private final TableDescription td;
    private final transient PropertyChangeListener pcl = new PropertyChangeListener() {
      @Override
      public void propertyChange(final PropertyChangeEvent evt) {
        refresh(true);
      }
    };
    private final transient PropertyChangeListener nodeChangeListener = new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        ic.add(td);
        td.rowFieldChanged(null, null, evt.getNewValue());
      }
    };

    public RowChildFactory(TableDescription td) {
      td.addPropertyChangeListener(pcl);
      this.td = td;
    }
    @Override
    protected  boolean createKeys(List<cz.lbenda.dbapp.rc.frm.RowNode.Row> toPopulate) {
      List<Object[]> list = td.getRows();
      for (Object[] val : list) {
        toPopulate.add(new cz.lbenda.dbapp.rc.frm.RowNode.Row(td, val));
      }
      return true;
    }
    @Override
    protected Node createNodeForKey(cz.lbenda.dbapp.rc.frm.RowNode.Row row) {
      RowNode node = new RowNode(row);
      node.addPropertyChangeListener(nodeChangeListener);
      return node;
    }
  }

}
