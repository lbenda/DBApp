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
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Blob;
import java.sql.SQLException;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 26.9.15. */
public class BlobBinaryData implements BinaryData {

  public static BlobBinaryData NULL = new BlobBinaryData("NULL", null);

  private final Blob blob;
  private final String name;

  public BlobBinaryData(String name, Blob blob) {
    this.name = name;
    this.blob = blob;
  }

  @Override
  public String getName() { return name; }

  @Override
  public InputStream getInputStream() {
    try {
      return blob.getBinaryStream();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Reader getReader() {
    return new InputStreamReader(getInputStream());
  }

  @Override
  public boolean isText() { return false; }
  @Override
  public boolean isNull() { return blob == null; }
  @Override
  public boolean isLazyLoading() { return true; }

  @Override
  public long size() {
    try {
      return blob == null ? 0 : blob.length();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BlobBinaryData)) return false;

    BlobBinaryData that = (BlobBinaryData) o;

    return !(blob != null ? !blob.equals(that.blob) : that.blob != null) && !(name != null ? !name.equals(that.name)
        : that.name != null);
  }

  @Override
  public int hashCode() {
    int result = blob != null ? blob.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }
}
