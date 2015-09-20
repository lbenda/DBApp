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

import javafx.util.StringConverter;

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
}
