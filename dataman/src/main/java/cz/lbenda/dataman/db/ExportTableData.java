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
package cz.lbenda.dataman.db;

import cz.lbenda.dataman.Constants;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 20.9.15.
 * Class which give ability to export data table */
public class ExportTableData {

  public enum SpreadsheetFormat {
    XLSX("xlsx"), XLS("xls"), ODS("ods"), CSV("csv"), TXT("txt"), SQL("sql"), ;
    private static Map<String, SpreadsheetFormat> extensionsMap = new HashMap<>();
    static {
      Arrays.stream(SpreadsheetFormat.values()).forEach(s -> extensionsMap.put(s.getExtension(), s));
    }
    private String extension; public String getExtension() { return extension; }
    SpreadsheetFormat(String extension) { this.extension = extension; }
    public static SpreadsheetFormat byExtension(String extension) { return extensionsMap.get(extension); }
  }

  public static void writeSqlQueryRows(String fileName, SQLQueryRows sqlQueryRows, String sheetName, OutputStream outputStream) throws IOException {
    String extension = FilenameUtils.getExtension(fileName);
    SpreadsheetFormat format = SpreadsheetFormat.byExtension(extension);
    if (format == null) { format = SpreadsheetFormat.CSV; }
    switch (format) {
      case XLSX: writeSqlQueryRowsToXLSX(sqlQueryRows, sheetName, outputStream); break;
      case XLS: writeSqlQueryRowsToXLS(sqlQueryRows, sheetName, outputStream); break;
      case CSV: writeSqlQueryRowsToCSV(sqlQueryRows, outputStream); break;
      case TXT: writeSqlQueryRowsToTXT(sqlQueryRows, outputStream); break;
      case ODS: writeSqlQueryRowsToODS(sqlQueryRows, sheetName, outputStream); break;
    }
  }

  /** Write rows to XLSX file
   * @param sqlQueryRows rows
   * @param sheetName name of sheet where is data write
   * @param outputStream stream where are data write */
  public static void writeSqlQueryRowsToXLSX(SQLQueryRows sqlQueryRows, String sheetName, OutputStream outputStream) throws IOException {
    XSSFWorkbook wb = new XSSFWorkbook();
    XSSFSheet sheet = wb.createSheet(sheetName) ;
    XSSFRow headerRow = sheet.createRow(0);
    int c = 0;
    for (ColumnDesc columnDesc : sqlQueryRows.getMetaData().getColumns()) {
      XSSFCell cell = headerRow.createCell(c);
      cell.setCellValue(columnDesc.getName());
      c++;
    }
    int r = 1;
    for (RowDesc row : sqlQueryRows.getRows()) {
      XSSFRow xlsxRow = sheet.createRow(r);
      c = 0;
      for (ColumnDesc columnDesc : sqlQueryRows.getMetaData().getColumns()) {
        XSSFCell cell = xlsxRow.createCell(c);
        cell.setCellValue(row.getColumnValueStr(columnDesc));
        c++;
      }
      r++;
    }
    wb.write(outputStream);
  }

  /** Write rows to XLS file
   * @param sqlQueryRows rows
   * @param sheetName name of sheet where is data write
   * @param outputStream stream where are data write */
  public static void writeSqlQueryRowsToXLS(SQLQueryRows sqlQueryRows, String sheetName, OutputStream outputStream) throws IOException {
    HSSFWorkbook wb = new HSSFWorkbook();
    HSSFSheet sheet = wb.createSheet(sheetName) ;
    HSSFRow headerRow = sheet.createRow(0);
    int c = 0;
    for (ColumnDesc columnDesc : sqlQueryRows.getMetaData().getColumns()) {
      HSSFCell cell = headerRow.createCell(c);
      cell.setCellValue(columnDesc.getName());
      c++;
    }
    int r = 1;
    for (RowDesc row : sqlQueryRows.getRows()) {
      HSSFRow xlsxRow = sheet.createRow(r);
      c = 0;
      for (ColumnDesc columnDesc : sqlQueryRows.getMetaData().getColumns()) {
        HSSFCell cell = xlsxRow.createCell(c);
        cell.setCellValue(row.getColumnValueStr(columnDesc));
        c++;
      }
      r++;
    }
    wb.write(outputStream);
  }

  /** Write rows to CSV file
   * @param sqlQueryRows rows
   * @param outputStream stream where are data write */
  public static void writeSqlQueryRowsToCSV(SQLQueryRows sqlQueryRows, OutputStream outputStream) throws IOException {
    CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(Constants.CSV_NEW_LINE_SEPARATOR);
    Writer writer = new OutputStreamWriter(outputStream);
    CSVPrinter csvFilePrinter = new CSVPrinter(writer, csvFileFormat);
    csvFilePrinter.printRecord(sqlQueryRows.getMetaData().getColumns().stream().map(ColumnDesc::getName).toArray());
    for (RowDesc row : sqlQueryRows.getRows()) {
      csvFilePrinter.printRecord(sqlQueryRows.getMetaData().getColumns().stream().map(row::getColumnValueStr).toArray());
    }
  }

  private static StringBuffer SPACES = new StringBuffer();

  private static String fixedString(String value, int length) {
    if (SPACES.length() < length) { for (int i = SPACES.length(); i < length; i++) { SPACES.append(" "); }}
    if (value == null) { return SPACES.substring(0, length); }
    return value + SPACES.substring(0, length - value.length());
  }

  /** Write rows to CSV file
   * @param sqlQueryRows rows
   * @param outputStream stream where are data write */
  public static void writeSqlQueryRowsToTXT(SQLQueryRows sqlQueryRows, OutputStream outputStream) throws IOException {
    Writer writer = new OutputStreamWriter(outputStream);
    String joined = sqlQueryRows.getMetaData().getColumns().stream()
        .map(cd -> fixedString(cd.getName(), cd.getSize()))
        .collect(Collectors.joining(""));
    writer.append(joined).append(Constants.CSV_NEW_LINE_SEPARATOR);
    for (RowDesc row : sqlQueryRows.getRows()) {
      joined = sqlQueryRows.getMetaData().getColumns().stream()
          .map(cd -> fixedString(row.getColumnValueStr(cd), cd.getSize()))
          .collect(Collectors.joining(""));
      writer.append(joined).append(Constants.CSV_NEW_LINE_SEPARATOR);
    }
  }

  /** Write rows to ODS file
   * @param sqlQueryRows rows
   * @param sheetName name of sheet where is data write
   * @param outputStream stream where are data write */
  public static void writeSqlQueryRowsToODS(SQLQueryRows sqlQueryRows, @SuppressWarnings("UnusedParameters") String sheetName, OutputStream outputStream) throws IOException {
    Object[] header = sqlQueryRows.getMetaData().getColumns().stream().map(ColumnDesc::getName).toArray();
    Object[][] rows = new Object[sqlQueryRows.getRows().size()][];
    int i = 0;
    for (RowDesc row : sqlQueryRows.getRows()) {
      rows[i] = sqlQueryRows.getMetaData().getColumns().stream().map(row::getColumnValue).toArray();
      i++;
    }
    TableModel model = new DefaultTableModel(rows, header);
    SpreadSheet.createEmpty(model).getPackage().save(outputStream);
  }
}
