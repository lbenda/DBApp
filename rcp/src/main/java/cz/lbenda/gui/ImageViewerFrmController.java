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
package cz.lbenda.gui;

import cz.lbenda.common.Tuple2;
import cz.lbenda.rcp.DialogHelper;
import cz.lbenda.rcp.localization.Message;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 26.9.15.
 * Pane for view image with some buttons around */
public class ImageViewerFrmController implements Initializable {

  private static final Logger LOG = LoggerFactory.getLogger(ImageViewerFrmController.class);
  private static final String FXML_RESOURCE = "ImageViewerFrm.fxml";

  @Message
  public static final String WINDOW_TITLE = "Image viewer";

  @FXML
  private ImageView imageView;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
  }

  public void setImage(Image image) {
    imageView.setImage(image);
  }

  /** Create new instance return main node and controller of this node and sub-nodes */
  public static Tuple2<Parent, ImageViewerFrmController> createNewInstance() {
    URL resource = ImageViewerFrmController.class.getResource(FXML_RESOURCE);
    try {
      FXMLLoader loader = new FXMLLoader();
      loader.setLocation(resource);
      loader.setBuilderFactory(new JavaFXBuilderFactory());
      Parent node = loader.load(resource.openStream());
      ImageViewerFrmController controller = loader.getController();
      return new Tuple2<>(node, controller);
    } catch (IOException e) {
      LOG.error("Problem with reading FXML", e);
      throw new RuntimeException("Problem with reading FXML", e);
    }
  }

  public static void openImageWindow(Stage parentStage, Image image) {
    openImageWindow(parentStage, image, WINDOW_TITLE);
  }

  public static void openImageWindow(Stage parentStage, Image image, String title) {
    Tuple2<Parent, ImageViewerFrmController> tuple2 = createNewInstance();
    tuple2.get2().setImage(image);
    DialogHelper.getInstance().openWindowInCenterOfStage(parentStage, (Pane) tuple2.get1(), title);
  }
}
