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
        category = "DBTable",
        id = "cz.lbenda.dbapp.actions.DBTableReloadAction"
)
@ActionRegistration(
        iconBase = "cz/lbenda/dbapp/actions/task-recurring.png",
        displayName = "#CTL_DBTableReloadAction"
)
@ActionReference(path = "Toolbars/DBTable", position = 900)
@Messages("CTL_DBTableReloadAction=Reload table data")
public final class DBTableReloadAction implements ActionListener {

  private final DBTableReloadCookie context;

  public DBTableReloadAction(DBTableReloadCookie context) {
    this.context = context;
  }

  @Override
  public void actionPerformed(ActionEvent ev) {
    context.reloadTable();
  }
}
