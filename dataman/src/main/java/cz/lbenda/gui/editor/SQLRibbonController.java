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
package cz.lbenda.gui.editor;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;

import java.net.URL;
import java.util.ResourceBundle;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 8.9.15.
 * Controller for ribbon menu for SQL editor */
public class SQLRibbonController implements Initializable {

  @FXML
  private Button sqlRun;

  /** Action listener which is call when user click on button sqlRun */
  public EventHandler<ActionEvent> getSqlRunAH() { return sqlRun.getOnAction(); }
  public void setSqlRunAH(EventHandler<ActionEvent> sqlRunAL) { sqlRun.setOnAction(sqlRunAL); }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    assert sqlRun != null;
  }
}
