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

import cz.lbenda.rcp.localization.Message;
import cz.lbenda.rcp.localization.MessageFactory;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 20.9.15.
 * Default string converters */
@SuppressWarnings("unused")
public class StringConverters {

  @Message
  public static final String MSG_BIG_VALUE = "<BIG VALUE>";
  @Message
  public static final String MSG_CHARACTER_VALUE = "<CHARACTER VALUE>";
  static {
    MessageFactory.initializeMessages(MSG_BIG_VALUE);
  }

  public static StringConverter<LocalDate> LOCALDATE_CONVERTER = new StringConverter<LocalDate>() {
    @Override public String toString(LocalDate value) { return value == null ? null : value.format(Constants.LOCAL_DATE_FORMATTER); }
    @Override public LocalDate fromString(String s) { return s == null ? null : LocalDate.parse(s, Constants.LOCAL_DATE_FORMATTER); }
  };

  public static StringConverter<LocalTime> LOCALTIME_CONVERTER = new StringConverter<LocalTime>() {
    @Override public String toString(LocalTime value) { return value == null ? null : value.format(Constants.LOCAL_TIME_FORMATTER); }
    @Override public LocalTime fromString(String s) { return s == null ? null : LocalTime.parse(s, Constants.LOCAL_TIME_FORMATTER); }
  };

  public static StringConverter<LocalDateTime> LOCALDATETIME_CONVERTER = new StringConverter<LocalDateTime>() {
    @Override public String toString(LocalDateTime value) { return value == null ? null : value.format(Constants.LOCAL_DATETIME_FORMATTER); }
    @Override public LocalDateTime fromString(String s) { return s == null ? null : LocalDateTime.parse(s, Constants.LOCAL_DATETIME_FORMATTER); }
  };


  public static StringConverter<Date> SQL_DATE_CONVERTER = new StringConverter<Date>() {
    @Override public String toString(Date value) { return value == null ? null : Constants.DATE_FORMATTER.get().format(value); }
    @Override public Date fromString(String s) {
      try {
        return s == null ? null : new Date(Constants.DATE_FORMATTER.get().parse(s).getTime());
      } catch (ParseException e) { throw new RuntimeException(e); }
    }
  };

  public static StringConverter<Time> SQL_TIME_CONVERTER = new StringConverter<Time>() {
    @Override public String toString(Time value) { return value == null ? null : Constants.TIME_FORMATTER.get().format(value); }
    @Override public Time fromString(String s) {
      try {
        return s == null ? null : new Time(Constants.TIME_FORMATTER.get().parse(s).getTime());
      } catch (ParseException e) { throw new RuntimeException(e); }
    }
  };

  public static StringConverter<Timestamp> SQL_TIMESTAMP_CONVERTER = new StringConverter<Timestamp>() {
    @Override public String toString(Timestamp value) { return value == null ? null : Constants.DATETIME_FORMATTER.get().format(value); }
    @Override public Timestamp fromString(String s) {
      try {
        return s == null ? null : new Timestamp(Constants.DATETIME_FORMATTER.get().parse(s).getTime());
      } catch (ParseException e) { throw new RuntimeException(e); }
    }
  };


  public static StringConverter<Integer> INT_CONVERTER = new StringConverter<Integer>() {
    @Override public String toString(Integer value) { return value == null ? null : value.toString(); }
    @Override public Integer fromString(String s) { return s == null ? null : Integer.parseInt(s); }
  };
  public static StringConverter<Byte> BYTE_CONVERTER = new StringConverter<Byte>() {
    @Override public String toString(Byte value) { return value == null ? null : value.toString(); }
    @Override public Byte fromString(String s) { return s == null ? null : Byte.parseByte(s); }
  };
  public static StringConverter<Short> SHORT_CONVERTER = new StringConverter<Short>() {
    @Override public String toString(Short value) { return value == null ? null : value.toString(); }
    @Override public Short fromString(String s) { return s == null ? null : Short.parseShort(s); }
  };
  public static StringConverter<Long> LONG_CONVERTER = new StringConverter<Long>() {
    @Override public String toString(Long value) { return value == null ? null : value.toString(); }
    @Override public Long fromString(String s) { return s == null ? null : Long.parseLong(s); }
  };
  public static StringConverter<BigDecimal> DECIMAL_CONVERTER = new StringConverter<BigDecimal>() {
    @Override public String toString(BigDecimal value) { return value == null ? null : value.toString(); }
    @Override public BigDecimal fromString(String s) { return s == null ? null : new BigDecimal(s); }
  };

  public static StringConverter<Float> FLOAT_CONVERTER = new StringConverter<Float>() {
    @Override public String toString(Float value) { return value == null ? null : value.toString(); }
    @Override public Float fromString(String s) { return s == null ? null : Float.parseFloat(s); }
  };
  public static StringConverter<Double> DOUBLE_CONVERTER = new StringConverter<Double>() {
    @Override public String toString(Double value) { return value == null ? null : value.toString(); }
    @Override public Double fromString(String s) { return s == null ? null : Double.parseDouble(s); }
  };

  public static StringConverter<String> STRING_CONVERTER = new StringConverter<String>() {
    @Override public String toString(String value) { return value; }
    @Override public String fromString(String s) { return s; }
  };
  public static StringConverter<Boolean> BOOLEAN_CONVERTER = new StringConverter<Boolean>() {
    @Override public String toString(Boolean value) { return value == null ? null : value.toString(); }
    @Override public Boolean fromString(String s) { return s == null ? null : Boolean.parseBoolean(s); }
  };

  public static StringConverter<Object> OBJECT_CONVERTER = new StringConverter<Object>() {
    @Override public String toString(Object value) { return value == null ? null : value.toString(); }
    @Override public Object fromString(String s) { throw new UnsupportedOperationException(); }
  };

  public static StringConverter<BinaryData> BINARYDATA_CONVERTER = new StringConverter<BinaryData>() {
    @Override public String toString(BinaryData value) {
      return value == null || value.isNull() ? null :
          value.isText() ? MSG_CHARACTER_VALUE : MSG_BIG_VALUE;
    }
    @Override public BinaryData fromString(String s) { throw new UnsupportedOperationException(); }
  };

  public static StringConverter<byte[]> BYTEARRAY_CONVERTER = new StringConverter<byte[]>() {
    @Override
    public String toString(byte[] bytes) {
      if (bytes == null || bytes.length == 0) { return null; }
      return DatatypeConverter.printHexBinary(bytes);
    }
    @Override
    public byte[] fromString(String s) {
      if (s == null || StringUtils.isBlank(s)) { return null; }
      return DatatypeConverter.parseHexBinary(s);
    }
  };

  @SuppressWarnings("unchecked")
  public static <T> StringConverter<T> converterForClass(Class<T> clazz) {
    if (Byte.class.isAssignableFrom(clazz)) { return (StringConverter<T>) BYTE_CONVERTER; }
    if (Short.class.isAssignableFrom(clazz)) { return (StringConverter<T>) SHORT_CONVERTER; }
    if (Integer.class.isAssignableFrom(clazz)) { return (StringConverter<T>) INT_CONVERTER; }
    if (Long.class.isAssignableFrom(clazz)) { return (StringConverter<T>) LONG_CONVERTER; }
    if (Float.class.isAssignableFrom(clazz)) { return (StringConverter<T>) FLOAT_CONVERTER; }
    if (Double.class.isAssignableFrom(clazz)) { return (StringConverter<T>) DOUBLE_CONVERTER; }
    if (BigDecimal.class.isAssignableFrom(clazz)) { return (StringConverter<T>) DECIMAL_CONVERTER; }

    if (String.class.isAssignableFrom(clazz)) { return (StringConverter<T>) STRING_CONVERTER; }

    if (Boolean.class.isAssignableFrom(clazz)) { return (StringConverter<T>) BOOLEAN_CONVERTER; }

    if (java.sql.Date.class.isAssignableFrom(clazz)) { return (StringConverter<T>) SQL_DATE_CONVERTER; }
    if (java.sql.Time.class.isAssignableFrom(clazz)) { return (StringConverter<T>) SQL_TIME_CONVERTER; }
    if (java.sql.Timestamp.class.isAssignableFrom(clazz)) { return (StringConverter<T>) SQL_TIMESTAMP_CONVERTER; }
    if (LocalDate.class.isAssignableFrom(clazz)) { return (StringConverter<T>) LOCALDATE_CONVERTER; }
    if (LocalDateTime.class.isAssignableFrom(clazz)) { return (StringConverter<T>) LOCALDATETIME_CONVERTER; }
    if (LocalTime.class.isAssignableFrom(clazz)) { return (StringConverter<T>) LOCALTIME_CONVERTER; }
    if (BinaryData.class.isAssignableFrom(clazz)) { return (StringConverter<T>) BINARYDATA_CONVERTER; }

    if (clazz.isArray()) {
      if (Byte.TYPE.isAssignableFrom(clazz.getComponentType())) { return (StringConverter<T>) BYTEARRAY_CONVERTER; }
    }


    return (StringConverter<T>) OBJECT_CONVERTER;
  }
}
