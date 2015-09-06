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
        id = "cz.lbenda.dbapp.actions.ExportAction"
)
@ActionRegistration(
        iconBase = "cz/lbenda/dbapp/actions/database-export.png",
        displayName = "#CTL_ExportAction"
)
@Messages("CTL_ExportAction=Export")
public final class ExportAction implements ActionListener {

  private final ExportCookie context;

  public ExportAction(ExportCookie context) {
    this.context = context;
  }

  @Override
  public void actionPerformed(ActionEvent ev) { context.export(); }
}
