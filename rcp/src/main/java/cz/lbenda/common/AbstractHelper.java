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

import java.nio.ByteBuffer;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.UUID;

/** Class which help with some work.
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public abstract class AbstractHelper {

  /** This method run equals on two object. If both is null, then is equal */
  public static boolean nullEquals(Object o1, Object o2) {
    if (o1 == null) { return o2 == null; }
    if (o2 == null) { return false; }
    if (o1.getClass().isArray()) {
      if (!o2.getClass().isArray()) { return false; }
      if (!o1.getClass().getComponentType().equals(o2.getClass().getComponentType())) { return false; }
      if (Byte.TYPE.equals(o1.getClass().getComponentType())) { return Arrays.equals((byte[]) o1, (byte[]) o2); }
      if (Short.TYPE.equals(o1.getClass().getComponentType())) { return Arrays.equals((short[]) o1, (short[]) o2); }
      if (Integer.TYPE.equals(o1.getClass().getComponentType())) { return Arrays.equals((int[]) o1, (int[]) o2); }
      if (Long.TYPE.equals(o1.getClass().getComponentType())) { return Arrays.equals((long[]) o1, (long[]) o2); }
      if (Boolean.TYPE.equals(o1.getClass().getComponentType())) { return Arrays.equals((boolean[]) o1, (boolean[]) o2); }
      if (Character.TYPE.equals(o1.getClass().getComponentType())) { return Arrays.equals((char[]) o1, (char[]) o2); }
      if (Double.TYPE.equals(o1.getClass().getComponentType())) { return Arrays.equals((double[]) o1, (double[]) o2); }
      if (Float.TYPE.equals(o1.getClass().getComponentType())) { return Arrays.equals((float[]) o1, (float[]) o2); }
      return Arrays.equals((Object[]) o1, (Object[]) o2);
    }
    boolean result = o1.equals(o2);
    if (!result) {
      if (o1 instanceof LocalTime && o2 instanceof Time) {
        result = o1.equals(((Time) o2).toLocalTime());
      } else if (o2 instanceof LocalTime && o1 instanceof Time) {
        result = o2.equals(((Time) o1).toLocalTime());
      } else if (o1 instanceof LocalDate && o2 instanceof Date) {
        result = o1.equals(((Date) o2).toLocalDate());
      } else if (o2 instanceof LocalDate && o1 instanceof Date) {
        result = o2.equals(((Date) o1).toLocalDate());
      } else if (o1 instanceof LocalDateTime && o2 instanceof Timestamp) {
        result = o1.equals(((Timestamp) o2).toLocalDateTime());
      } else if (o2 instanceof LocalDateTime && o1 instanceof Timestamp) {
        result = o2.equals(((Timestamp) o1).toLocalDateTime());
      }
    }
    return result;
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
        if (!o1[i].equals(o2[i])) {
          //noinspection unchecked
          return o1[i].compareTo(o2[i]);
        }
      }
    }
    return 0;
  }

  /** Transform given uuid to byte array. If uuid is null then return null. */
  public static byte[] uuidToByteArray(UUID uuid) {
    if (uuid == null) { return null; }
    ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }

  /** Transform bite array to bit representation
   * @param bytes array of bites which will be interpreted to binary map
   * @return string like this: 0110 1111 0101 1000 */
  public static String printBitBinary(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      String s = Integer.toBinaryString((b & 0xFF) + 0x100).substring(1);
      if (sb.length() > 0) { sb.append(" "); }
      if (s.length() > 4) { sb.append(s.substring(0, 4)).append(" ").append(s.substring(4, s.length())); }
      else { sb.append(s); }
    }
    return sb.toString();
  }

  /** Parse binary map from string to array of bytes
   * @param bytes String which hold byte array. The space is removed before transformation.
   * @return byte array */
  public static byte[] parseBitBinary(String bytes) {
    bytes = bytes.replaceAll(" ", "");
    if (bytes.length() % 8 > 0) { bytes += "00000000".substring(0, bytes.length() % 8); }
    byte[] result = new byte[bytes.length() / 8];

    for (int i = 0; i < result.length; i ++) {
      String s = bytes.substring(i * 8, (i + 1) * 8);
      short sh = Short.parseShort(s, 2);
      result[i] = (byte) sh;
    }
    return result;
  }
}
