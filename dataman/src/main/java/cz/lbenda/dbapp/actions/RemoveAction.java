/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.actions;

import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@ActionID(
        category = "DBStructure",
        id = "cz.lbenda.dbapp.actions.RemoveAction"
)
@ActionRegistration(
        iconBase = "cz/lbenda/dbapp/actions/database-remove.png",
        displayName = "#CTL_RemoveAction"
)
@Messages("CTL_RemoveAction=Remove")
public final class RemoveAction implements ActionListener {

  private final RemoveCookie context;

  public RemoveAction(RemoveCookie context) {
    this.context = context;
  }

  @Override
  public void actionPerformed(ActionEvent ev) { context.remove(); }
}
