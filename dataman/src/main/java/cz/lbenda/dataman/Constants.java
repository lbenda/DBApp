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
package cz.lbenda.dataman;

import cz.lbenda.rcp.localization.Message;
import cz.lbenda.rcp.localization.MessageFactory;
import javafx.stage.FileChooser.ExtensionFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 13.9.15.
 * Constants for application dataman */
public class Constants {

  public static String CONFIG_EXTENSION = "dtm";
  public static String SQL_EXTENSION = "sql";
  public static String XLSX_EXTENSION = "xlsx";
  public static String CSV_EXTENSION = "csv";
  public static String TXT_EXTENSION = "txt";
  public static String EXPORT_SHEET_NAME = "Dataman";

  @Message
  public static final String CSV_NEW_LINE_SEPARATOR = "\n";

  public static List<ExtensionFilter> configFileFilter = new ArrayList<>(Arrays.asList(
      new ExtensionFilter[] {
          new ExtensionFilter("Dataman config file (DTM, XML)", "*.dtm", "*.xml"),
          new ExtensionFilter("All files", "*.*")
      }));
  public static List<ExtensionFilter> librariesFilter = new ArrayList<>(Arrays.asList(
      new ExtensionFilter[] {
          new ExtensionFilter("Java libraries (jar, war, ear, zip)", "*.jar", "*.war", "*.ear", "*.zip"),
          new ExtensionFilter("All files", "*.*")
      }));
  public static List<ExtensionFilter> sqlFilter = new ArrayList<>(Arrays.asList(
      new ExtensionFilter[] {
          new ExtensionFilter("SQL files (sql, txt)", "*.sql", "*.txt"),
          new ExtensionFilter("All files", "*.*")
      }));
  public static List<ExtensionFilter> spreadSheetFilter = new ArrayList<>(Arrays.asList(
      new ExtensionFilter[] {
          new ExtensionFilter("All spreadsheets (ODS, XLSX, XLS, CSV, TXT)", "*.ods", "*.xlsx", "*.xls", "*.csv", "*.txt"),
          new ExtensionFilter("Open document format (ODS)", "*.ods"),
          new ExtensionFilter("Excel XLSX files (XLSX)", "*.xlsx", "*.xls"),
          new ExtensionFilter("Comma separated values (CSV)", "*.csv"),
          new ExtensionFilter("Fixed length column (TXT)", "*.txt"),
          new ExtensionFilter("All files", "*.*")
      }));

  static {
    MessageFactory.initializeMessages(Constants.class);
  }
}
