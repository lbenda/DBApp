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
package cz.lbenda.rcp.ribbon;

import cz.lbenda.rcp.ribbon.skin.RibbonMainButtonSkin;
import javafx.scene.control.Control;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Skin;
import javafx.scene.image.Image;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 24.9.15.
 * Controller for button which is show on left top corner of window */
public class RibbonMainButton extends Control {
  public final static String DEFAULT_STYLE_CLASS = "main-button";

  private Menu menu;
  private String appName; public String getAppName() { return appName; }
  private Image appImg; public Image getAppImg() { return appImg; }

  /** Create main ribbon button */
  public RibbonMainButton(String appName, Image appImg) {
    getStyleClass().setAll(DEFAULT_STYLE_CLASS);
    this.appName = appName;
    this.appImg = appImg;
  }

  @Override
  protected Skin<?> createDefaultSkin() {
    return new RibbonMainButtonSkin(this);
  }

  public Menu getMenu() { return menu; }
}
