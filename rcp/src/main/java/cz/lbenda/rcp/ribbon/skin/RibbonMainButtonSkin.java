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
package cz.lbenda.rcp.ribbon.skin;

import cz.lbenda.rcp.IconFactory;
import cz.lbenda.rcp.ribbon.RibbonMainButton;
import javafx.scene.control.Button;
import javafx.scene.control.SkinBase;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 24.9.15.
 * Skin for main button on ribbon */
public class RibbonMainButtonSkin extends SkinBase<RibbonMainButton> {

  public RibbonMainButtonSkin(RibbonMainButton control) {
    super(control);
    HBox pane = new HBox();
    Button menu = new Button(control.getAppName(), new ImageView(control.getAppImg()));
    Button dropDown = new Button(null, IconFactory.getInstance().imageView(this, "dropDown.png",
        IconFactory.IconSize.XSMALL)) ;
    dropDown.setOnAction(event -> {
      control.getMenu().show(dropDown,
          dropDown.getScene().getWindow().getX() + menu.getScene().getX() + menu.getLayoutX(),
          dropDown.getScene().getWindow().getY() + menu.getScene().getX() + menu.getLayoutY() + menu.getHeight() + menu.getHeight()); // + menu.getHeight());
    });
    menu.setOnAction(dropDown.getOnAction()); // TODO create separate window
    pane.getChildren().addAll(menu, dropDown);
    getChildren().add(pane);
  }
}
