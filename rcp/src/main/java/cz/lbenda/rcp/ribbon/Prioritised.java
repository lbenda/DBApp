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

import java.util.Comparator;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 20.9.15.
 * Prioritised object. Used on example for order or hide or show the button. The lowest number priority is higher.
 * NULL mean no priority at all */
public interface Prioritised {
  Comparator<Prioritised> COMPARATOR = (p1, p2) ->
        p1.getPriority() == null && p2.getPriority() == null ? 0 :
            p1.getPriority() == null ? 1 :
                p2.getPriority() == null ? -1 :
                    p1.getPriority().compareTo(p2.getPriority());

  /** The lowest number priority is higher NULL mean no priority at all */
  Integer getPriority();
}
