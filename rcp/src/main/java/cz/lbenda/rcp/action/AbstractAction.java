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

import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 12.9.15.
 * Abstract ancestor of Action */
public abstract class AbstractAction implements Action {

  private List<Consumer<Boolean>> enableDisableConsumers = new ArrayList<>();
  private List<Consumer<Integer>> changeActionConfigConsumers = new ArrayList<>();

  private boolean enable = true;
  private int config = 0; public int getConfig() { return config; }

  @Override
  public void addEnableDisableConsumer(Consumer<Boolean> consumer) { this.enableDisableConsumers.add(consumer); }
  @Override
  public void removeEnableDisableConsumer(Consumer<Boolean> consumer) { this.enableDisableConsumers.remove(consumer); }

  @Override
  public void addChangeActionConfigConsumer(Consumer<Integer> consumer) { this.changeActionConfigConsumers.add(consumer); }
  @Override
  public void removeChangeActionConfigConsumer(Consumer<Integer> consumer) { this.changeActionConfigConsumers.remove(consumer); }

  @Override
  public boolean isEnable() { return enable; }
  public void setEnable(boolean enable) {
    this.enable = enable;
    enableDisableConsumers.forEach(consumer -> consumer.accept(enable));
  }
  public void setConfig(int config) {
    changeActionConfigConsumers.forEach(consumer -> consumer.accept(config));
  }
}
