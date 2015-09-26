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
import javafx.stage.FileChooser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

  /** Thousand which is used for getting metric prefix in bytes */
  public static long byteThousand = 1024;
  /** Metric prefix but only for thousands and bigger then 1 */
  public static String[] METRIC_THOUSAND_PREFIX = new String[] { "k", "M", "G", "T" };
  public static long[] BYTES_THOUSAND = new long[] {
      byteThousand,
      byteThousand * byteThousand,
      byteThousand * byteThousand * byteThousand,
      byteThousand * byteThousand * byteThousand * byteThousand,
      byteThousand * byteThousand * byteThousand * byteThousand * byteThousand };
  /** For given size in bytes return prefix which is used for better readibility */
  public static String prefixForSize(long size) {
    if (byteThousand > size) { return ""; }
    if (byteThousand * byteThousand > size) { return METRIC_THOUSAND_PREFIX[0]; }
    if (byteThousand * byteThousand * byteThousand > size) { return METRIC_THOUSAND_PREFIX[1]; }
    if (byteThousand * byteThousand * byteThousand * byteThousand > size) { return METRIC_THOUSAND_PREFIX[2]; }
    return METRIC_THOUSAND_PREFIX[3];
  }
  /** Transform given size in bytes to size which work with prefix together */
  public static long transformToPrefixedSize(long size) {
    if (byteThousand > size) { return size; }
    if (byteThousand * byteThousand > size) { return size / byteThousand; }
    if (byteThousand * byteThousand * byteThousand > size) { return size / (byteThousand * byteThousand); }
    if (byteThousand * byteThousand * byteThousand * byteThousand > size) { return size / (byteThousand * byteThousand * byteThousand); }
    return size / (byteThousand * byteThousand * byteThousand * byteThousand);
  }

  /** Standard height of window which is open to edit text */
  public static double TextAreaWindowsHeight = 500;
  /** Standard weight of window which is open to edit text */
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

  /** If field have this size, then can be showed text area instead of text field */
  public static int MIN_SIZE_FOR_TEXT_AREA = 100;
  /** Prefer height of text area */
  public static double TEXT_AREA_PREF_HIGH = 50.0;

  public static List<FileChooser.ExtensionFilter> allFilesFilter = new ArrayList<>(Arrays.asList(
      new FileChooser.ExtensionFilter[] { new FileChooser.ExtensionFilter("All files", "*.*") }));

  static {
    MessageFactory.initializeMessages(Constants.class);
    if (CONF_DATE_FORMAT != null) { LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern(CONF_DATE_FORMAT); }
  }
}
