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
package cz.lbenda.rcp;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.InputStream;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 19.9.15.
 * Factory for creating icons */
public class IconFactory {

  private static final Logger LOG = LoggerFactory.getLogger(IconFactory.class);

  /** Size of icon which will be used */
  public enum IconSize {
    XSMALL(12), SMALL(16), MEDIUM(24), LARGE(32), XLARGE(48) ;
    int size;
    IconSize(int size) { this.size = size; }
    public int size() { return size; }
  }

  /** Location where is icon used, by this location is size choose */
  public enum IconLocation {
    INDICATOR(IconSize.XSMALL), MENU_ITEM(IconSize.SMALL), GLOBAL_TOOL_BAR(IconSize.XLARGE), LOCAL_TOOLBAR(IconSize.MEDIUM),
    TABLE_CELL(IconSize.SMALL), ;
    private IconSize iconSize;
    IconLocation(IconSize iconSize) { this.iconSize = iconSize; }
    public IconSize getIconSize() { return iconSize; }
    public void setIconSize(IconSize iconSize) { this.iconSize = iconSize; }
  }

  private static IconFactory iconFactory;

  public static IconFactory getInstance() {
    if (iconFactory == null) { iconFactory = new IconFactory(); }
    return iconFactory;
  }

  /** Return image view for given base name. The icon is get from caller class */
  @SuppressWarnings("unused")
  public <T> ImageView imageView(@Nonnull T caller, @Nonnull String baseName, @Nonnull IconSize iconSize) {
    return new ImageView(image(caller, baseName, iconSize));
  }

  /** Return image view for given base name. The icon is get from caller class */
  public <T> ImageView imageView(@Nonnull T caller, @Nonnull String baseName, @Nonnull IconLocation location) {
    return new ImageView(image(caller, baseName, location.getIconSize()));
  }

  private String iconName(String base, IconSize iconSize) {
    if (iconSize != null) {
      String ext = FilenameUtils.getExtension(base);
      return FilenameUtils.removeExtension(base) + iconSize.size() + (StringUtils.isBlank(ext) ? "" : "." + ext);
    }
    return base;
  }

  public <T> Image image(@Nonnull T caller, @Nonnull String base, @Nonnull IconLocation location) {
    return image(caller, base, location.getIconSize());
  }

  public <T> Image image(@Nonnull T caller, String base, IconSize iconSize) {
    final Class clazz;
    if (caller instanceof Class) { clazz = (Class) caller; }
    else { clazz = caller.getClass(); }
    String iconName = iconName(base, iconSize);
    InputStream is = clazz.getResourceAsStream(iconName);
    if (is == null) {
      String iconName2 = iconName("unknown.png", iconSize);
      LOG.warn("Icon with name not exist '" + iconName + "' '" + iconName2 + "' used instead of.");
      is = IconFactory.class.getResourceAsStream(iconName2);
    }
    return new Image(is);
  }
}
