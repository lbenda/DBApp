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
package cz.lbenda.dbapp.rc;

/** Class which help with some work.
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public abstract class AbstractHelper {

  /** This method run equals on two object. If both is null, then is equal */
  public static boolean nullEquals(Object o1, Object o2) {
    if (o1 == null) { return o2 == null; }
    return o1.equals(o2);
  }

  /** Compare method for more then one object. If the first object is equal (or both are null)
   * then is compare next item. If both object in arra is null then is equal, if one isn't null then is greater. */
  public static int compareArrayNull(Comparable[] o1, Comparable[] o2) {
    if (o1 == null) { throw new NullPointerException("Parameter o1 is null"); }
    if (o2 == null) { throw new NullPointerException("Parameter o2 is null"); }
    if (o1.length != o2.length) { throw new IllegalArgumentException("The parameter o1 and o2 have different length"); }
    for (int i = 0; i < o1.length; i++) {
      if (!nullEquals(o1[i], o2[i])) {
        if (o1[i] == null) { return -1; }
        else if (o2[i] == null) { return 1; }
        if (!o1[i].equals(o2[i])) { return o1[i].compareTo(o2[i]); }
      }
    }
    return 0;
  }

  public static boolean isEmpty(String str) {
    return str == null || "".equals(str);
  }
  public static boolean isBlank(String str) {
    return str == null || "".equals(str.trim());
  }
}
