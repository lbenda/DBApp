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
package cz.lbenda.common;

import java.io.InputStream;
import java.io.Reader;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 26.9.15.
 * Interface which implements binary data object */
public interface BinaryData {
  /** Return name of binary date */
  String getName();
  InputStream getInputStream();
  Reader getReader();
  boolean isText();
  long size();
  boolean isLazyLoading();
  /** Return information if object represent null or not */
  boolean isNull();
}
