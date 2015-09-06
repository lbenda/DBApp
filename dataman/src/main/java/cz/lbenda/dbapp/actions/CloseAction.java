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
        id = "cz.lbenda.dbapp.actions.CloseAction"
)
@ActionRegistration(
        iconBase = "cz/lbenda/dbapp/actions/database-close.png",
        displayName = "#CTL_CloseAction"
)
@Messages("CTL_CloseAction=Close")
public final class CloseAction implements ActionListener {

  private final CloseCookie context;

  public CloseAction(CloseCookie context) {
    this.context = context;
  }

  @Override
  public void actionPerformed(ActionEvent ev) { context.close(); }
}
