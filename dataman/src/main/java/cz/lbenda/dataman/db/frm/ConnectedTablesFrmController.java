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
package cz.lbenda.dataman.db.frm;

import cz.lbenda.common.AbstractHelper;
import cz.lbenda.dataman.db.ColumnDesc;
import cz.lbenda.dataman.db.DbStructureFactory;
import cz.lbenda.dataman.db.RowDesc;
import cz.lbenda.dataman.db.TableDesc;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 16.9.15.
 * Controller which tables which are connected with rows on curernt table. */
public class ConnectedTablesFrmController {

  private Map<TableDesc, DataTableFrmController> slaveDTFC = new WeakHashMap<>();
  private ScrollPane mainPane = new ScrollPane();
  private VBox vBox = new VBox();
  @Nonnull
  public ScrollPane getMainPane() { return mainPane; }
  private Map<DbStructureFactory.ForeignKey, Predicate<RowDesc>> filters = new ConcurrentHashMap<>();

  public ConnectedTablesFrmController(@Nonnull DataTableView masterTableView) {
    mainPane.setContent(vBox);
    prepareView(masterTableView.getTableDesc());
    masterTableView.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) ->
        selectRow(masterTableView.getTableDesc(), (RowDesc) newValue));
  }

  private void prepareView(@Nonnull TableDesc masterTD) {
    for (DbStructureFactory.ForeignKey fk : masterTD.getForeignKeys()) {
      TableDesc mtd = fk.getMasterTable();
      TableDesc std = fk.getSlaveTable();
      TableDesc slaveTD = masterTD.equals(mtd) ? std : mtd;
      DataTableFrmController dtFrm = new DataTableFrmController(slaveTD);
      TitledPane titledPane = new TitledPane();
      titledPane.setText(slaveTD.getName());
      titledPane.setContent(dtFrm.getTabView());
      titledPane.setExpanded(false);
      vBox.getChildren().add(titledPane);
      slaveDTFC.put(slaveTD, dtFrm);
    }
  }

  private void selectRow(@Nonnull TableDesc masterTD, RowDesc row) {
    for (DbStructureFactory.ForeignKey fk : masterTD.getForeignKeys()) {
      Predicate<RowDesc> predicate = filters.get(fk);

      TableDesc mtd = fk.getMasterTable();
      TableDesc std = fk.getSlaveTable();
      TableDesc slaveTD = masterTD.equals(mtd) ? std : mtd;
      ColumnDesc slaveCD = masterTD.equals(mtd) ? fk.getSlaveColumn() : fk.getMasterColumn();
      ColumnDesc masterCD = masterTD.equals(mtd) ? fk.getMasterColumn() : fk.getSlaveColumn();

      DataTableFrmController dtFrm = slaveDTFC.get(slaveTD);

      if (predicate != null) { dtFrm.getTabView().filters().remove(predicate); }
      predicate = fr -> row != null && fr != null && AbstractHelper.nullEquals(row.getColumnValue(masterCD), fr.getColumnValue(slaveCD));
      dtFrm.getTabView().filters().add(predicate);
      filters.put(fk, predicate);
    }
  }
}
