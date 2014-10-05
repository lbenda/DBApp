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
        id = "cz.lbenda.dbapp.actions.DBTableCancelAction"
)
@ActionRegistration(
        iconBase = "cz/lbenda/dbapp/actions/dialog-cancel.png",
        displayName = "#CTL_DBTableCancelAction"
)
@ActionReference(path = "Toolbars/DBTable", position = 700)
@Messages("CTL_DBTableCancelAction=Cancel changes")
public final class DBTableCancelAction implements ActionListener {

  private final DBTableCancelCookie context;

  public DBTableCancelAction(DBTableCancelCookie context) {
    this.context = context;
  }

  @Override
  public void actionPerformed(ActionEvent ev) {
    context.cancelChanges();
  }
}
