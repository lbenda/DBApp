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
package cz.lbenda.dataman.rc;

import javafx.scene.Node;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 16.9.15.
 * Tab descriptor */
public class DetailDescriptor {
  public String title; public String getTitle() { return title; }
  public Node node; public Node getNode() { return node; }
  public boolean closable; public boolean getClosable() { return closable; }

  public DetailDescriptor(String title, Node node, boolean closable) {
    this.title = title;
    this.node = node;
    this.closable = closable;
  }
}
