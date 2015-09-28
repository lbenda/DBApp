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

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import java.util.function.Consumer;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 12.9.15.
 * This interface is implemented by action which can be lunch in menu */
public interface Action extends EventHandler<ActionEvent> {
  /** Add consumer which is inform when action is enabled/disabled */
  void addEnableDisableConsumer(Consumer<Boolean> consumer);
  /** Remove consumer which is inform when action is enabled/disabled */
  void removeEnableDisableConsumer(Consumer<Boolean> consumer);

  /** Add consumer which is inform when action want change configuration */
  void addChangeActionConfigConsumer(Consumer<Integer> consumer);
  /** Remove consumer which is inform when action want change configuration */
  void removeChangeActionConfigConsumer(Consumer<Integer> consumer);

  /** Return enable if action is enable */
  boolean isEnable();
}
