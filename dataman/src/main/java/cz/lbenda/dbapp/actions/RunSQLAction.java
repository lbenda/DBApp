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
        category = "SQLEditor",
        id = "cz.lbenda.dbapp.actions.RunSQLAction"
)
@ActionRegistration(
        iconBase = "cz/lbenda/dbapp/actions/sqlRun.png",
        displayName = "#CTL_RunSQLAction"
)
@Messages("CTL_RunSQLAction=Run SQL")
public final class RunSQLAction implements ActionListener {

  private final RunSQLCookie context;

  public RunSQLAction(RunSQLCookie context) {
    this.context = context;
  }

  @Override
  public void actionPerformed(ActionEvent ev) { context.run(); }
}
