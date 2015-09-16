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
package cz.lbenda.rcp.action;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Lukas Benda <lbenda @ lbenda.cz> on 12.9.15.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ActionConfig {
  /** Identifies category of an action. The category can be separate by "/" separator
   * @return string representing programmatic name of the category
   */
  String category();
  /** The unique ID (inside a category) of the action. Should follow
   * Java naming conventions and somehow include package name prefix. Like
   * <code>org.myproject.myproduct.MyAction</code>.
   *
   * @return java identifiers separated with '.'
   */
  String id();
  /** Priority of show action */
  int priority();
  /** Array of gui configs */
  ActionGUIConfig[] gui() default {};
}
