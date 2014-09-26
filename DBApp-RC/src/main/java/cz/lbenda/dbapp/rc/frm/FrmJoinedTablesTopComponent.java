/*
 * Copyright 2014 Lukas Benda <lbenda at lbenda.cz>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.lbenda.dbapp.rc.frm;

import cz.lbenda.dbapp.rc.db.DbStructureReader.ForeignKey;
import cz.lbenda.dbapp.rc.db.TableDescription;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//cz.lbenda.applicationdb.rc.frm//FrmJoinedTables//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "FrmJoinedTablesTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "output", openAtStartup = true)
@ActionID(category = "Window", id = "cz.lbenda.applicationdb.rc.frm.FrmJoinedTablesTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_FrmJoinedTablesAction",
        preferredID = "FrmJoinedTablesTopComponent"
)
@Messages({
  "CTL_FrmJoinedTablesAction=Spojené tabulky",
  "CTL_FrmJoinedTablesTopComponent=Spojené tabulky",
  "HINT_FrmJoinedTablesTopComponent=Tabulky napojené na právě vybranou tabulku - cizím klíčem."
})
public final class FrmJoinedTablesTopComponent extends TopComponent implements ChosenTable.ChosenTableListener {

  public FrmJoinedTablesTopComponent() {
    initComponents();
    setName(Bundle.CTL_FrmJoinedTablesTopComponent());
    setToolTipText(Bundle.HINT_FrmJoinedTablesTopComponent());

  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jTabbedPane1 = new javax.swing.JTabbedPane();

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
    );
  }// </editor-fold>//GEN-END:initComponents

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JTabbedPane jTabbedPane1;
  // End of variables declaration//GEN-END:variables
  @Override
  public void componentOpened() {
    // jTree1.setRootVisible(false);
    ChosenTable.getInstance().addTableListener(this);
  }

  @Override
  public void componentClosed() {
  }

  void writeProperties(java.util.Properties p) {
    // better to version settings since initial version as advocated at
    // http://wiki.apidesign.org/wiki/PropertyFiles
    p.setProperty("version", "1.0");
    // TODO store your settings
  }

  void readProperties(java.util.Properties p) {
    String version = p.getProperty("version");
    // TODO read your settings according to their version
  }

  @Override
  public void tableChosen(TableDescription tableDescription) {
    jTabbedPane1.removeAll();
    for (ForeignKey key : tableDescription.getForeignKeys()) {
      if (key.getMasterTable().equals(tableDescription)) {
        jTabbedPane1.addTab(key.getSlaveTable().getName(), new PanelJoinTable(key, false));
      } else {
        jTabbedPane1.addTab(String.format("%s (%s)", key.getSlaveColumn().getName(), key.getMasterTable().getName()),
                new PanelJoinTable(key, true));
      }
    }
  }
}
