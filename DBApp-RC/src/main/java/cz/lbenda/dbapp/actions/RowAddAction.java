/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.actions;

import cz.lbenda.dbapp.rc.db.TableDescription;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Edit",
        id = "cz.lbenda.dbapp.actions.RowAddAction"
)
@ActionRegistration(
        iconBase = "cz/lbenda/dbapp/actions/edit-table-insert-row-under.png",
        displayName = "#CTL_RowAddAction"
)
@ActionReferences({
  @ActionReference(path = "Menu/Edit", position = 2200, separatorBefore = 2150),
  @ActionReference(path = "Toolbars/File", position = 500)
})
@Messages("CTL_RowAddAction=Add new row to table")
public final class RowAddAction implements ActionListener {

  private final TableDescription context;

  public RowAddAction(TableDescription context) {
    this.context = context;
  }

  @Override
  public void actionPerformed(ActionEvent ev) {
    // TODO use context
  }
}
