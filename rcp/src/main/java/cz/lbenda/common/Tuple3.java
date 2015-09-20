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

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 8.9.15. */
@SuppressWarnings("unused")
public class Tuple3<T, F, S> {

  private T o1; public T get1() { return o1; } public void set1(T o1) { this.o1 = o1; }
  private F o2; public F get2() { return o2; } public void set2(F o2) { this.o2 = o2; }
  private S o3; public S get3() { return o3; } public void set3(S o3) { this.o3 = o3; }

  public Tuple3() {}

  public Tuple3(T o1, F o2, S o3) {
    this.o1 = o1;
    this.o2 = o2;
    this.o3 = o3;
  }

  @Override
  public String toString() {
    return "Tuple3{" +
        "o1=" + o1 +
        ", o2=" + o2 +
        ", o3=" + o3 +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Tuple3)) return false;

    Tuple3<?, ?, ?> tuple3 = (Tuple3<?, ?, ?>) o;

    return !(o1 != null ? !o1.equals(tuple3.o1) : tuple3.o1 != null)
        && !(o2 != null ? !o2.equals(tuple3.o2) : tuple3.o2 != null)
        && !(o3 != null ? !o3.equals(tuple3.o3) : tuple3.o3 != null);
  }

  @Override
  public int hashCode() {
    int result = o1 != null ? o1.hashCode() : 0;
    result = 31 * result + (o2 != null ? o2.hashCode() : 0);
    result = 31 * result + (o3 != null ? o3.hashCode() : 0);
    return result;
  }
}
