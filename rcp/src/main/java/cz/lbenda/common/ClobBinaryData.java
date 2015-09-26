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
import java.sql.Clob;
import java.sql.SQLException;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 26.9.15. */
public class ClobBinaryData implements BinaryData {

  public static ClobBinaryData NULL = new ClobBinaryData("NULL", null);

  private final Clob clob;
  private final String name;

  public ClobBinaryData(String name, Clob clob) {
    this.name = name;
    this.clob = clob;
  }

  @Override
  public String getName() { return name; }
  @Override
  public InputStream getInputStream() {
    try {
      return clob.getAsciiStream();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Reader getReader() {
    try {
      return clob.getCharacterStream();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isText() { return true; }
  @Override
  public boolean isNull() { return clob == null; }
  @Override
  public boolean isLazyLoading() { return true; }

  @Override
  public long size() {
    try {
      return clob == null ? 0 : clob.length();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ClobBinaryData)) return false;

    ClobBinaryData that = (ClobBinaryData) o;

    return !(clob != null ? !clob.equals(that.clob) : that.clob != null) && !(name != null ? !name.equals(that.name)
        : that.name != null);
  }

  @Override
  public int hashCode() {
    int result = clob != null ? clob.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }
}
