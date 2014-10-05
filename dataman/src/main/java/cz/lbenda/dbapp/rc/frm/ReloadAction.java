/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.rc.frm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "DBStructure",
        id = "cz.lbenda.dbapp.rc.frm.ReloadAction"
)
@ActionRegistration(
        iconBase = "cz/lbenda/dbapp/rc/frm/task-recurring.png",
        displayName = "#CTL_ReloadAction"
)
@Messages("CTL_ReloadAction=Reload")
public final class ReloadAction implements ActionListener {

  private final ReloadCookie context;

  public ReloadAction(ReloadCookie context) {
    this.context = context;
  }

  @Override
  public void actionPerformed(ActionEvent ev) {
    context.reload();
  }
}
