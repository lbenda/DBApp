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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 20.9.15.
 * Constatnst for RCP */
@SuppressWarnings("unused")
public class Constants {

  @Message
  public static String CONF_DATE_FORMAT;
  @Message
  public static String CONF_DATETIME_FORMAT;
  @Message
  public static String CONF_TIME_FORMAT;

  public static double TextAreaWindowsHeight = 500;
  public static double TextAreaWindowsWeight = 800;

  public static DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
  public static ThreadLocal<DateFormat> DATE_FORMATTER = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      if (CONF_DATE_FORMAT != null) {
        //noinspection ConstantConditions
        return new SimpleDateFormat(CONF_DATE_FORMAT);
      } else { return DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()); }
    }
  };
  public static DateTimeFormatter LOCAL_DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  public static ThreadLocal<DateFormat> DATETIME_FORMATTER = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      if (CONF_DATETIME_FORMAT != null) {
        //noinspection ConstantConditions
        return new SimpleDateFormat(CONF_DATETIME_FORMAT);
      } else { return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()); }
    }
  };
  public static DateTimeFormatter LOCAL_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
  public static ThreadLocal<DateFormat> TIME_FORMATTER = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      if (CONF_TIME_FORMAT != null) {
        //noinspection ConstantConditions
        return new SimpleDateFormat(CONF_TIME_FORMAT);
      } else { return DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()); }
    }
  };

  static {
    MessageFactory.initializeMessages(Constants.class);
    if (CONF_DATE_FORMAT != null) { LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern(CONF_DATE_FORMAT); }
  }
}
