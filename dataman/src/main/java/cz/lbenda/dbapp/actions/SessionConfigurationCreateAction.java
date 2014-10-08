/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.actions;

import cz.lbenda.dbapp.rc.SessionConfiguration;
import cz.lbenda.dbapp.rc.frm.StructureChildFactory.SessionConfigurationNode;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.IntrospectionException;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "DBStructure/Session/Create",
        id = "cz.lbenda.dbapp.actions.SessionConfigurationCreateAction"
)
@ActionRegistration(
        iconBase = "cz/lbenda/dbapp/rc/frm/database-add.png",
        displayName = "#CTL_SessionConfigurationCreateAction"
)
@Messages("CTL_SessionConfigurationCreateAction=Create session configuration")
public final class SessionConfigurationCreateAction implements ActionListener {

  private static final Logger LOG = LoggerFactory.getLogger(SessionConfigurationCreateAction.class);

  @Override
  public void actionPerformed(ActionEvent e) {
    LOG.debug("actionPerformed");
    SessionConfiguration sc = SessionConfiguration.newConfiguration();
    try {
      SessionConfigurationNode node = new SessionConfigurationNode(sc);
      SessionConfiguration.registerNewConfiguration(sc);
      e.setSource(node);
      LOG.debug("Fire customization");
      node.getPreferredAction().actionPerformed(e);
    } catch (IntrospectionException ex) {
      LOG.error("Faild the open configuration for new session", ex);
      throw new RuntimeException("Faild the open configuration for new session", ex);
    }
  }
}
