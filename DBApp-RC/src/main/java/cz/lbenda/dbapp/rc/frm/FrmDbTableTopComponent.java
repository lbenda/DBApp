/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.rc.frm;

import cz.lbenda.dbapp.rc.db.Column;
import cz.lbenda.dbapp.rc.db.TableDescription;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import javax.swing.Action;
import javax.swing.CellEditor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import org.netbeans.swing.etable.ETable;
import org.netbeans.swing.etable.ETableColumn;
import org.netbeans.swing.etable.ETableColumnModel;
import org.netbeans.swing.outline.DefaultOutlineCellRenderer;
import org.openide.awt.HtmlRenderer;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.OutlineView;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top component which displays something.
 */
@TopComponent.Description(
        preferredID = "FrnDbTableTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
// @ActionID(category = "Window", id = "cz.lbenda.dbapp.rc.frm.FrnDbTableTopComponent")
// @ActionReference(path = "Menu/Window" /*, position = 333 */)
@Messages({
  "CTL_FrnDbTableAction=FrnDbTable",
  "CTL_FrnDbTableTopComponent=FrnDbTable Window",
  "HINT_FrnDbTableTopComponent=This is a FrnDbTable window"
})
public final class FrmDbTableTopComponent extends TopComponent implements ExplorerManager.Provider {

  private static final Logger LOG = LoggerFactory.getLogger(FrmDbTableTopComponent.class);
  private ExplorerManager em = new ExplorerManager();
  private final TableDescription td;
  private final OutlineView ov;
  private final RowChildFactory childFactory;

  public FrmDbTableTopComponent(TableDescription td) {
    this.td = td;
    initComponents();

    Action rowAdd = FileUtil.getConfigObject("Actions/Edit/cz-lbenda-dbapp-actions-RowAddAction.instance", Action.class);

    getActionMap().put("add", rowAdd);
    for (Object keys : getActionMap().allKeys()) {
      LOG.trace("Keys in action map: " + keys);
    }
    // setName(Bundle.CTL_FrnDbTableTopComponent());
    // setToolTipText(Bundle.HINT_FrnDbTableTopComponent());
    setHtmlDisplayName(generateTitle());
    jPanel1.setLayout(new BorderLayout());
    childFactory = new RowChildFactory(td);
    Children kids = Children.create(childFactory, true);
    Node rootNode = new AbstractNode(kids);
    ov = new OutlineView();
    ov.getOutline().setRootVisible(false);
    ov.getOutline().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    ((ETableColumnModel) ov.getOutline().getColumnModel()).setColumnHidden(((ETableColumn) ov.getOutline().getColumnModel().getColumn(0)), true);
    String[] ar = new String[td.getColumns().size() * 2];
    int i = 0;
    for (Column column : td.getColumns()) {
      ar[i] = column.getName();
      ar[i + 1] = column.getName();
      i += 2;
    }
    ov.setPropertyColumns(ar);
    ov.getOutline().setDefaultRenderer(Node.Property.class, new ColumnCellRenderer(td));
    // setCellEditors(ov);
    jPanel1.add(ov, BorderLayout.CENTER);
    em.setRootContext(rootNode);
    associateLookup(ExplorerUtils.createLookup(em, getActionMap()));
    for (Column col : td.getColumns()) {
      CellEditor ce = ((ETable) ov.getOutline()).getCellEditor(1, col.getPosition() + 1);
      LOG.trace("Celle editor for column: " + col.getName() + " is " + ce.getClass());
      // ((ETable) ov.getOutline()).setCellEditor(null);
    }
  }

  TableModelListener tableModelListener = new TableModelListener() {
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

  private String generateTitle() {
    StringBuilder title = new StringBuilder("<html><body><small><small>");
    title.append(td.getSessionConfiguration().getId());
    title.append("</small></small> ");
    if (td.getSessionConfiguration().isShowCatalog(td.getCatalog())) { title.append(td.getCatalog()).append("."); }
    if (td.getSessionConfiguration().isShowSchema(td.getSchema(), td.getCatalog())) { title.append(td.getSchema()).append("."); }
    title.append("<b>").append(td.getName()).append("</b></body></html>");
    return title.toString();
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jToolBar1 = new javax.swing.JToolBar();
    bSave = new javax.swing.JButton();
    bAdd = new javax.swing.JButton();
    bRemove = new javax.swing.JButton();
    bCancel = new javax.swing.JButton();
    bReload = new javax.swing.JButton();
    jPanel1 = new javax.swing.JPanel();

    jToolBar1.setRollover(true);

    bSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cz/lbenda/dbapp/rc/frm/document-save.png"))); // NOI18N
    org.openide.awt.Mnemonics.setLocalizedText(bSave, org.openide.util.NbBundle.getMessage(FrmDbTableTopComponent.class, "FrmDbTableTopComponent.bSave.text")); // NOI18N
    bSave.setToolTipText(org.openide.util.NbBundle.getMessage(FrmDbTableTopComponent.class, "FrmDbTableTopComponent.bSave.toolTipText")); // NOI18N
    bSave.setFocusable(false);
    bSave.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    bSave.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    bSave.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        bSaveActionPerformed(evt);
      }
    });
    jToolBar1.add(bSave);

    bAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cz/lbenda/dbapp/rc/frm/edit-table-insert-row-under.png"))); // NOI18N
    org.openide.awt.Mnemonics.setLocalizedText(bAdd, org.openide.util.NbBundle.getMessage(FrmDbTableTopComponent.class, "FrmDbTableTopComponent.bAdd.text")); // NOI18N
    bAdd.setToolTipText(org.openide.util.NbBundle.getMessage(FrmDbTableTopComponent.class, "FrmDbTableTopComponent.bAdd.toolTipText")); // NOI18N
    bAdd.setFocusable(false);
    bAdd.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    bAdd.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    bAdd.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        bAddActionPerformed(evt);
      }
    });
    jToolBar1.add(bAdd);

    bRemove.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cz/lbenda/dbapp/rc/frm/edit-table-delete-row.png"))); // NOI18N
    org.openide.awt.Mnemonics.setLocalizedText(bRemove, org.openide.util.NbBundle.getMessage(FrmDbTableTopComponent.class, "FrmDbTableTopComponent.bRemove.text")); // NOI18N
    bRemove.setToolTipText(org.openide.util.NbBundle.getMessage(FrmDbTableTopComponent.class, "FrmDbTableTopComponent.bRemove.toolTipText")); // NOI18N
    bRemove.setFocusable(false);
    bRemove.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    bRemove.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    bRemove.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        bRemoveActionPerformed(evt);
      }
    });
    jToolBar1.add(bRemove);

    bCancel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cz/lbenda/dbapp/rc/frm/dialog-cancel.png"))); // NOI18N
    org.openide.awt.Mnemonics.setLocalizedText(bCancel, org.openide.util.NbBundle.getMessage(FrmDbTableTopComponent.class, "FrmDbTableTopComponent.bCancel.text")); // NOI18N
    bCancel.setToolTipText(org.openide.util.NbBundle.getMessage(FrmDbTableTopComponent.class, "FrmDbTableTopComponent.bCancel.toolTipText")); // NOI18N
    bCancel.setFocusable(false);
    bCancel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    bCancel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    bCancel.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        bCancelActionPerformed(evt);
      }
    });
    jToolBar1.add(bCancel);

    bReload.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cz/lbenda/dbapp/rc/frm/task-recurring.png"))); // NOI18N
    org.openide.awt.Mnemonics.setLocalizedText(bReload, org.openide.util.NbBundle.getMessage(FrmDbTableTopComponent.class, "FrmDbTableTopComponent.bReload.text")); // NOI18N
    bReload.setToolTipText(org.openide.util.NbBundle.getMessage(FrmDbTableTopComponent.class, "FrmDbTableTopComponent.bReload.toolTipText")); // NOI18N
    bReload.setFocusable(false);
    bReload.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    bReload.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    bReload.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        bReloadActionPerformed(evt);
      }
    });
    jToolBar1.add(bReload);

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 400, Short.MAX_VALUE)
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 269, Short.MAX_VALUE)
    );

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
      .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
  }// </editor-fold>//GEN-END:initComponents

  private void bAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bAddActionPerformed
    ov.getOutline().getModel().addTableModelListener(tableModelListener);
    td.createRow();
    ov.getOutline().unsetQuickFilter();
    // ov.getOutline().getModel().removeTableModelListener(tableModelListener);
  }//GEN-LAST:event_bAddActionPerformed

  private void bRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bRemoveActionPerformed
    td.removeRows(ov.getOutline().getSelectedRows());
  }//GEN-LAST:event_bRemoveActionPerformed

  private void bCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bCancelActionPerformed
    td.cancelChanges();
  }//GEN-LAST:event_bCancelActionPerformed

  private void bSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bSaveActionPerformed
    td.saveChanges();
  }//GEN-LAST:event_bSaveActionPerformed

  private void bReloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bReloadActionPerformed
    td.reloadRows();
  }//GEN-LAST:event_bReloadActionPerformed

  @Override
  public void componentActivated() {
    super.componentActivated();
    ChosenTable.getInstance().setTableDescription(td);
    ExplorerUtils.activateActions(em, true);
  }

  @Override
  public void componentDeactivated() {
    ExplorerUtils.activateActions(em, false);
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton bAdd;
  private javax.swing.JButton bCancel;
  private javax.swing.JButton bReload;
  private javax.swing.JButton bRemove;
  private javax.swing.JButton bSave;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JToolBar jToolBar1;
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
      RowNode node = null;
      try {
        node = new RowNode(row);
      } catch (IntrospectionException ex) {
        LOG.error("Faild to create node.", ex);
        Exceptions.printStackTrace(ex);
      }
      return node;
    }
  }

  private static class ColumnCellRenderer extends DefaultOutlineCellRenderer {
    /**
     * Gray Color for the odd lines in the view.
     */
    private static final Color VERY_LIGHT_GRAY = new Color(236, 236, 236);
    /**
     * Center the content of the cells displaying text.
     */
    protected boolean centered = System.getProperty("os.name").toLowerCase().indexOf("windows") < 0;
    /**
     * Highlight the non editable cells making the foreground lighter.
     */
    protected boolean lighterEditableFields = false;
    private final TableDescription td;

    public ColumnCellRenderer(TableDescription td) {
      this.td = td;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Component getTableCellRendererComponent(final JTable table,
                                                   final Object value,
                                                   final boolean isSelected,
                                                   final boolean hasFocus,
                                                   final int row,
                                                   final int column) {
      Component cell = null;
      Object valueToDisplay = value;
      Class valueClasss = td.getColumns().get(column - 1).getDataType().getDataType(); // First column is NODE
      if (value instanceof Node.Property) {
        try {
          valueToDisplay = ((Node.Property) value).getValue();
        } catch (IllegalAccessException ex) {
          Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
          Exceptions.printStackTrace(ex);
        }
      }
      TableCellRenderer renderer = table.getDefaultRenderer(valueClasss);
      if (renderer != null) {
        cell = renderer.getTableCellRendererComponent(table, valueToDisplay, isSelected,
            hasFocus, row, column);
      } else {
        cell = super.getTableCellRendererComponent(table, valueToDisplay, isSelected, hasFocus, row, column);
      }
      if (cell != null) {
        if (centered) {
          if (cell instanceof HtmlRenderer.Renderer) {
            ((HtmlRenderer.Renderer) cell).setCentered(centered);
          } else if (cell instanceof DefaultTableCellRenderer.UIResource) {
            ((DefaultTableCellRenderer.UIResource) cell).setHorizontalAlignment(JLabel.CENTER);
          }
        }
        Color foregroundColor = table.getForeground();
        int modelRow = table.convertRowIndexToModel(row);
        int modelColumn = table.convertColumnIndexToModel(column);
        final boolean cellEditable = table.getModel().isCellEditable(modelRow, modelColumn);
        if (lighterEditableFields && cellEditable) {
          foregroundColor = Color.BLUE;
        }
        cell.setForeground(foregroundColor);
        cell.setBackground(row % 2 == 0 ? Color.WHITE : VERY_LIGHT_GRAY);
        if (isSelected) {
          if (lighterEditableFields && cellEditable) {
            cell.setFont(cell.getFont().deriveFont(Font.BOLD));
          }
          cell.setBackground(table.getSelectionBackground());
        }
      }
      return cell;
    }

    /**
     * @return true if the text rendered in labels is centered.
     */
    public boolean isCentered() {
      return centered;
    }

    /**
     * @return true if non editable cells have a lighter foreground.
     */
    public boolean isLighterEditableFields() {
      return lighterEditableFields;
    }

    /**
     * Highlight the non editable cells making the foreground lighter.
     *
     * @param value true to activate this feature.
     */
    public void setLighterEditableFields(final boolean value) {
      this.lighterEditableFields = value;
    }
  }
}
