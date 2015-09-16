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

import cz.lbenda.rcp.localization.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 8.9.15.
 * Default controller for ribbon menu with pane for application */
public class RibbonController {

  private static final Logger LOG = LoggerFactory.getLogger(RibbonController.class);

  private final Ribbon ribbon; public Ribbon getRibbon() { return ribbon; }

  public RibbonController(MessageFactory messageFactory) {
    ribbon = new Ribbon(messageFactory);
  }
}
