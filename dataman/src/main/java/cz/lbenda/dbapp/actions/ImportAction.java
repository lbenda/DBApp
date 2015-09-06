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
        id = "cz.lbenda.dbapp.actions.ImportAction"
)
@ActionRegistration(
        iconBase = "cz/lbenda/dbapp/actions/database-import.png",
        displayName = "#CTL_ImportAction"
)
@Messages("CTL_ImportAction=Import")
public final class ImportAction implements ActionListener {

  private final ImportCookie context;

  public ImportAction(ImportCookie context) {
    this.context = context;
  }

  @Override
  public void actionPerformed(ActionEvent ev) { context.importing(); }
}
