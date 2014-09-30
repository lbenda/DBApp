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
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "DBTable",
        id = "cz.lbenda.dbapp.actions.RowAddAction"
)
@ActionRegistration(
        iconBase = "cz/lbenda/dbapp/actions/edit-table-insert-row-under.png",
        displayName = "#CTL_RowAddAction"
)
@ActionReferences({
  @ActionReference(path = "Toolbars/DBTable", position = 500)
})
@Messages("CTL_RowAddAction=Add new row to table")
public final class DBTableRowAddAction implements ActionListener {

  private final DBTableRowAddCookie context;

  public DBTableRowAddAction(DBTableRowAddCookie context) {
    this.context = context;
  }

  @Override
  public void actionPerformed(ActionEvent ev) {
    context.addRow();
  }
}
