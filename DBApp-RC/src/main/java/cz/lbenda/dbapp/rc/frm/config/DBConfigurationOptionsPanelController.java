/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.rc.frm.config;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

@OptionsPanelController.SubRegistration(
        location = "DBApp",
        displayName = "#AdvancedOption_DisplayName_DBConfiguration",
        keywords = "#AdvancedOption_Keywords_DBConfiguration",
        keywordsCategory = "DBApp/DBConfiguration"
)
@org.openide.util.NbBundle.Messages({"AdvancedOption_DisplayName_DBConfiguration=DB Configuration", "AdvancedOption_Keywords_DBConfiguration=DB Configuration"})
public final class DBConfigurationOptionsPanelController extends OptionsPanelController {

  private DBConfigurationPanel panel;
  private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
  private boolean changed;

  public static String PREF_NAME = "SessionConfiguration.configurations";

  public static String getConfigurationStr() {
    return NbPreferences.forModule(DBConfigurationPanel.class).get(PREF_NAME, null);
  }

  public static void saveConfigurationStr(String configuration) {
    NbPreferences.forModule(DBConfigurationPanel.class)
        .put(PREF_NAME, configuration);
  }

  public void update() {
    getPanel().load();
    changed = false;
  }

  public void applyChanges() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        getPanel().store();
        changed = false;
      }
    });
  }

  public void cancel() {
    // need not do anything special, if no changes have been persisted yet
  }

  public boolean isValid() {
    return getPanel().valid();
  }

  public boolean isChanged() {
    return changed;
  }

  public HelpCtx getHelpCtx() {
    return null; // new HelpCtx("...ID") if you have a help set
  }

  public JComponent getComponent(Lookup masterLookup) {
    return getPanel();
  }

  public void addPropertyChangeListener(PropertyChangeListener l) {
    pcs.addPropertyChangeListener(l);
  }

  public void removePropertyChangeListener(PropertyChangeListener l) {
    pcs.removePropertyChangeListener(l);
  }

  private DBConfigurationPanel getPanel() {
    if (panel == null) {
      panel = new DBConfigurationPanel(this);
    }
    return panel;
  }

  void changed() {
    if (!changed) {
      changed = true;
      pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
    }
    pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
  }

}
