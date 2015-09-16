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
package cz.lbenda.dataman.rc.homeMenu;

import cz.lbenda.rcp.action.AbstractAction;
import cz.lbenda.rcp.action.ActionConfig;
import cz.lbenda.rcp.action.ActionGUIConfig;
import cz.lbenda.rcp.localization.Message;
import javafx.event.ActionEvent;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15. */
@ActionConfig(
    category = "/HOME/Files",
    id = "cz.lbenda.dataman.rc.homeMenu.OpenFileHandler",
    priority = 100,
    gui = @ActionGUIConfig(
        displayName = @Message(id = "OpenFile", msg = "Open"),
        displayTooltip = @Message(id="OpenFile_tooltip", msg="Open file with SQL commands"),
        iconBase = "document-open.png"
    )
)
public class OpenFileHandler extends AbstractAction {
  @Override
  public void handle(ActionEvent actionEvent) {
    System.out.println("openFileHandler");
  }
}
