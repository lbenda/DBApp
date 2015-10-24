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
package cz.lbenda.common.io;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 18.10.15.
 * Output stream with byte buffer as result */
public class ByteBufferOutputStream extends OutputStream {
  private ByteBuffer buf;

  public ByteBufferOutputStream(ByteBuffer buf) {
    this.buf = buf;
  }

  public void write(int b) throws IOException {
    buf.put((byte) b);
  }

  public void write(@Nonnull byte[] bytes, int off, int len) throws IOException {
    buf.put(bytes, off, len);
  }
}
