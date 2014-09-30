/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "DBStructure",
        id = "cz.lbenda.dbapp.actions.DBTableRowsRemoveAction"
)
@ActionRegistration(
        iconBase = "cz/lbenda/dbapp/actions/edit-table-delete-row.png",
        displayName = "#CTL_DBTableRowsRemoveAction"
)
@ActionReference(path = "Toolbars/DBTable", position = 600)
@Messages("CTL_DBTableRowsRemoveAction=Remove rows")
public final class DBTableRowsRemoveAction implements ActionListener {

  private final DBTableRowsRemoveCookie context;

  public DBTableRowsRemoveAction(DBTableRowsRemoveCookie context) {
    this.context = context;
  }

  @Override
  public void actionPerformed(ActionEvent ev) {
    context.removeRows();
  }
}
