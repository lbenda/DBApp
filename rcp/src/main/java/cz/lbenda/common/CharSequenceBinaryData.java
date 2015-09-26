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

import org.apache.commons.io.input.CharSequenceReader;
import org.apache.commons.io.input.ReaderInputStream;

import java.io.InputStream;
import java.io.Reader;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 26.9.15.
 * Binary implementation of binary data which is in memory */
public class CharSequenceBinaryData implements BinaryData {

  public static CharSequenceBinaryData NULL = new CharSequenceBinaryData("NULL", null);

  private CharSequence charSequence;
  private String name;

  public CharSequenceBinaryData(String name, CharSequence charSequence) {
    this.name = name;
    this.charSequence = charSequence;
  }

  @Override
  public String getName() { return name; }
  @Override
  public InputStream getInputStream() { return new ReaderInputStream(getReader()); }
  @Override
  public Reader getReader() { return new CharSequenceReader(charSequence); }
  @Override
  public boolean isText() { return true; }
  @Override
  public boolean isLazyLoading() { return false; }

  @Override
  public long size() {
    if (charSequence == null) { return 0; }
    return charSequence.length();
  }

  @Override
  public boolean isNull() { return charSequence == null; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CharSequenceBinaryData)) return false;

    CharSequenceBinaryData that = (CharSequenceBinaryData) o;

    return !(charSequence != null ? !charSequence.equals(that.charSequence) : that.charSequence != null)
        && !(name != null ? !name.equals(that.name) : that.name != null);

  }

  @Override
  public int hashCode() {
    int result = charSequence != null ? charSequence.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }
}
