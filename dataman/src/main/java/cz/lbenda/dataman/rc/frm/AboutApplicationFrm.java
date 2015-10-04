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
package cz.lbenda.dataman.rc.frm;

import cz.lbenda.rcp.DialogHelper;
import cz.lbenda.rcp.localization.Message;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 28.9.15.
 * Frame which show information about application */
public class AboutApplicationFrm implements Initializable {

  private static final Logger LOG = LoggerFactory.getLogger(AboutApplicationFrm.class);
  private static final String FXML_RESOURCE = "AboutApplication.fxml";

  @Message
  public static final String WINDOW_TITLE = "About application";

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {

  }

  public static void show(Stage parentStage) {
    /** Create new instance return main node and controller of this node and subnodes */
    URL resource = AboutApplicationFrm.class.getResource(FXML_RESOURCE);
    try {
      FXMLLoader loader = new FXMLLoader();
      loader.setLocation(resource);
      loader.setBuilderFactory(new JavaFXBuilderFactory());
      Pane node = loader.load(resource.openStream());
      // AboutApplicationFrm controller = loader.getController();
      DialogHelper.getInstance().openWindowInCenterOfStage(parentStage, node, WINDOW_TITLE);
    } catch (IOException e) {
      LOG.error("Problem with reading FXML", e);
      throw new RuntimeException("Problem with reading FXML", e);
    }
  }
}
