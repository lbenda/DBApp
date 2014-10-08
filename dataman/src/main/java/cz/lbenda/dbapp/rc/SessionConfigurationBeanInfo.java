/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.rc;

import cz.lbenda.dbapp.rc.frm.config.SingleConfigurationPanel;
import java.beans.BeanDescriptor;
import java.beans.SimpleBeanInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Bean info object for session configuration
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public class SessionConfigurationBeanInfo extends SimpleBeanInfo {

  private static final Logger LOG = LoggerFactory.getLogger(SessionConfigurationBeanInfo.class);

  @Override
  public BeanDescriptor getBeanDescriptor() {
    return new BeanDescriptor(SessionConfiguration.class, SingleConfigurationPanel.class);
  }
}
