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
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 26.9.15.
 * Binary implementation of binary data which is in memory */
public class BitArrayBinaryData implements BinaryData {

  public static BitArrayBinaryData NULL = new BitArrayBinaryData("NULL", "");

  private byte[] bytes;
  private String name;

  public BitArrayBinaryData(String name, byte[] bytes) {
    this.name = name;
    this.bytes = bytes;
  }

  public BitArrayBinaryData(String name, String bytes) {
    this.name = name;
    if (StringUtils.isBlank(bytes)) { this.bytes = null; }
    else { this.bytes = AbstractHelper.parseBitBinary(bytes); }
  }

  @Override
  public String getName() { return name; }
  @Override
  public InputStream getInputStream() {
    return new ByteArrayInputStream(bytes);
  }
  @Override
  public Reader getReader() { return new CharSequenceReader(AbstractHelper.printBitBinary(bytes)); }
  @Override
  public boolean isText() { return true; }
  @Override
  public boolean isLazyLoading() { return false; }

  @Override
  public long size() {
    if (bytes == null) { return 0; }
    return bytes.length;
  }

  @Override
  public boolean isNull() { return bytes == null; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BitArrayBinaryData)) return false;

    BitArrayBinaryData that = (BitArrayBinaryData) o;

    return Arrays.equals(bytes, that.bytes);
  }

  @Override
  public int hashCode() {
    return bytes != null ? Arrays.hashCode(bytes) : 0;
  }
}
