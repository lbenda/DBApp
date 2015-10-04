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
import cz.lbenda.dataman.schema.export.*;
import cz.lbenda.dataman.schema.export.ColumnType;
import cz.lbenda.rcp.DialogHelper;
import cz.lbenda.rcp.ExceptionMessageFrmController;
import cz.lbenda.rcp.localization.Message;
import cz.lbenda.rcp.localization.MessageFactory;
import freemarker.cache.TemplateLoader;
import freemarker.template.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import javax.annotation.Nonnull;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 20.9.15.
 * Class which give ability to export data table */
public class ExportTableData {

  private static final Logger LOG = LoggerFactory.getLogger(ExportTableData.class);

  @Message
  public static final String CHOOSE_FORMAT = "Choose format for export";
  static { MessageFactory.initializeMessages(ExportTableData.class); }

  public enum SpreadsheetFormat {
    XLSX("xlsx"), XLS("xls"), ODS("ods"), CSV("csv"), TXT("txt"), XMLv1("xml"), XMLv2("xml"), SQL("sql"), ;
    private static Map<String, List<SpreadsheetFormat>> extensionsMap = new HashMap<>();
    static {
      Arrays.stream(SpreadsheetFormat.values()).forEach(s -> {
        List<SpreadsheetFormat> list = extensionsMap.get(s.getExtension());
        if (list == null) {
          list = new ArrayList<>();
          extensionsMap.put(s.getExtension(), list);
        }
        list.add(s);
      });
    }
    private String extension; public String getExtension() { return extension; }
    SpreadsheetFormat(String extension) { this.extension = extension; }
    public static List<SpreadsheetFormat> byExtension(String extension) { return extensionsMap.get(extension); }
  }
  public enum TemplateFormat {
    XLST_XMLv1, XLST_XMLv2, /* VELOCITY, */FREEMARKER, THYMELEAF, ;
  }

  public static class TemplateExportConfig {
    /** Format system for template */
    private ExportTableData.TemplateFormat templateFormat; public ExportTableData.TemplateFormat getTemplateFormat() { return templateFormat; }
    /** File which contains template */
    private String templateFile; public String getTemplateFile() { return templateFile; }
    /** File to which will be export write */
    private String file; public String getFile() { return file; }
    public TemplateExportConfig(ExportTableData.TemplateFormat templateFormat, String templateFile, String file) {
      this.templateFormat = templateFormat;
      this.templateFile = templateFile;
      this.file = file;
    }
  }

  public static void writeSqlQueryRows(String fileName, SQLQueryRows sqlQueryRows, String sheetName, OutputStream outputStream) throws IOException {
    String extension = FilenameUtils.getExtension(fileName);
    List<SpreadsheetFormat> formatList = SpreadsheetFormat.byExtension(extension);
    final SpreadsheetFormat format;
    if (formatList == null) { format = DialogHelper.chooseSingOption(CHOOSE_FORMAT, SpreadsheetFormat.values()); }
    else if (formatList.size() == 1) { format = formatList.get(0); }
    else { format = DialogHelper.chooseSingOption(CHOOSE_FORMAT, formatList); }
    if (format == null) { return; }
    switch (format) {
      case XLSX: writeSqlQueryRowsToXLSX(sqlQueryRows, sheetName, outputStream); break;
      case XLS: writeSqlQueryRowsToXLS(sqlQueryRows, sheetName, outputStream); break;
      case XMLv1: writeSqlQueryRowsToXMLv1(sqlQueryRows, outputStream); break;
      case XMLv2: writeSqlQueryRowsToXMLv2(sqlQueryRows, outputStream); break;
      case CSV: writeSqlQueryRowsToCSV(sqlQueryRows, outputStream); break;
      case TXT: writeSqlQueryRowsToTXT(sqlQueryRows, outputStream); break;
      case ODS: writeSqlQueryRowsToODS(sqlQueryRows, sheetName, outputStream); break;
    }
  }

  public static void writeSqlQueryRows(@Nonnull TemplateFormat format, @Nonnull SQLQueryRows sqlQueryRows,
                                       @Nonnull String templateFile, @Nonnull OutputStream outputStream)
      throws IOException {
    try (FileInputStream fis = new FileInputStream(templateFile)) {
      switch (format) {
        case FREEMARKER:
          writeSqlQueryRowsToFreemarker(sqlQueryRows, fis, outputStream);
          break;
        case THYMELEAF:
          writeSqlQueryRowsToThymeleaf(sqlQueryRows, templateFile, outputStream);
          break;
        /*   case VELOCITY: writeSqlQueryRowsToXMLv1(sqlQueryRows, template, outputStream); break;*/
        case XLST_XMLv1:
          writeSqlQueryRowsToXLSTXMLv1(sqlQueryRows, fis, outputStream);
          break;
        case XLST_XMLv2:
          writeSqlQueryRowsToXLSTXMLv2(sqlQueryRows, fis, outputStream);
          break;
      }
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

  private static DataTypeType columnTypeToDataTypeType(cz.lbenda.dataman.db.dialect.ColumnType columnType) {
    return DataTypeType.fromValue(columnType.name());
  }

  private static String columnId(ColumnDesc cd) {
    return cd.getCatalog() + "." + cd.getSchema() + "." + cd.getTable() + "." + cd.getName();
  }

  /** Write rows from sql query to output stream
   * @param sqlQueryRows rows
   * @param outputStream stream to which are data write */
  public static void writeSqlQueryRowsToXMLv1(SQLQueryRows sqlQueryRows, OutputStream outputStream) {
    ObjectFactory of = new ObjectFactory();
    ExportType export = of.createExportType();
    export.setSql(sqlQueryRows.getSQL());
    export.setVersion("1");
    ColumnsType columnsType = of.createColumnsType();
    export.setColumns(columnsType);
    Map<ColumnDesc, ColumnType> ctsMap = new HashMap<>();

    sqlQueryRows.getMetaData().getColumns().forEach(cd -> {
      ColumnType ct = of.createColumnType();
      ctsMap.put(cd, ct);
      ct.setId(columnId(cd));
      ct.setCatalog(cd.getCatalog());
      ct.setSchema(cd.getSchema());
      ct.setTable(cd.getTable());
      ct.setColumn(cd.getName());
      ct.setDataType(columnTypeToDataTypeType(cd.getDataType()));
      ct.setLength(cd.getSize());
      ct.setScale(cd.getScale());
      ct.setValue(cd.getLabel());
      columnsType.getColumn().add(ct);
    });

    sqlQueryRows.getRows().forEach(row -> {
      RowType rowType = of.createRowType();
      sqlQueryRows.getMetaData().getColumns().forEach(cd -> {
        FieldType field = of.createFieldType();
        field.setColumn(ctsMap.get(cd));
        if (row.isColumnNull(cd)) { field.setNull(true); }
        else { field.setValue(row.getColumnValueStr(cd)); }
        rowType.getField().add(field);
      });
      export.getRow().add(rowType);
    });

    try {
      JAXBContext jc = JAXBContext.newInstance(cz.lbenda.dataman.schema.export.ObjectFactory.class);
      Marshaller m = jc.createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
      m.marshal(of.createExport(export), outputStream);
    } catch (JAXBException e) {
      LOG.error("Problem with write exporting data: " + e.toString(), e);
      throw new RuntimeException("Problem with write exporting data: " + e.toString(), e);
    }
  }

  /** Write rows from sql query to output stream
   * @param sqlQueryRows rows
   * @param outputStream stream to which are data write */
  public static void writeSqlQueryRowsToXMLv2(SQLQueryRows sqlQueryRows, OutputStream outputStream) throws IOException {
    Element root = new Element("export");
    root.setAttribute("sql", sqlQueryRows.getSQL());
    root.setAttribute("version", "2");
    List<Element> rows = new ArrayList<>(sqlQueryRows.getRows().size());

    sqlQueryRows.getRows().forEach(rowDesc -> {
      Element row = new Element("row");
      List<Element> cols = new ArrayList<>(sqlQueryRows.getMetaData().getColumns().size());
      sqlQueryRows.getMetaData().getColumns().forEach(columnDesc -> {
        Element element = new Element(columnDesc.getName());
        if (rowDesc.isColumnNull(columnDesc)) { element.setAttribute("null", "true"); }
        else { element.setText(rowDesc.getColumnValueStr(columnDesc)); }
        cols.add(element);
      });
      row.addContent(cols);
      rows.add(row);
    });
    root.addContent(rows);
    XMLOutputter xmlOutputter = new XMLOutputter();
    xmlOutputter.output(new Document(root), outputStream);
  }

  private static void transformXSLT(InputStream templateStream, ByteArrayOutputStream xmlOutputStream, OutputStream outputStream) {
    StreamSource styleSource = new StreamSource(templateStream);
    ByteArrayInputStream bais = new ByteArrayInputStream(xmlOutputStream.toByteArray());
    StreamSource xmlSource = new StreamSource(bais);
    StreamResult streamResult = new StreamResult(outputStream);
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer(styleSource);
      transformer.transform(xmlSource, streamResult);
    } catch (TransformerConfigurationException e) {
      LOG.error("Problem with load style source.", e);
      ExceptionMessageFrmController.showException("Problem with load style source.", e);
    } catch (TransformerException e) {
      LOG.error("Problem with transforming input stream.", e);
      ExceptionMessageFrmController.showException("Problem with transforming input stream.", e);
    }
  }

  /** Write rows from sql query to output stream
   * @param sqlQueryRows rows
   * @param outputStream stream to which are data write */
  public static void writeSqlQueryRowsToXLSTXMLv1(SQLQueryRows sqlQueryRows, InputStream template, OutputStream outputStream) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    writeSqlQueryRowsToXMLv1(sqlQueryRows, baos);
    transformXSLT(template, baos, outputStream);
  }

  /** Write rows from sql query to output stream
   * @param sqlQueryRows rows
   * @param outputStream stream to which are data write */
  public static void writeSqlQueryRowsToXLSTXMLv2(SQLQueryRows sqlQueryRows, InputStream tempalte, OutputStream outputStream) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    writeSqlQueryRowsToXMLv2(sqlQueryRows, baos);
    transformXSLT(tempalte, baos, outputStream);
  }

  /** Write rows from sql query to output stream
   * @param sqlQueryRows rows
   * @param outputStream stream to which are data write */
  public static void writeSqlQueryRowsToThymeleaf(SQLQueryRows sqlQueryRows, String templateFileName, OutputStream outputStream) throws IOException {
    Context ctx = new Context(Locale.getDefault());
    ctx.setVariable("sql", sqlQueryRows.getSQL());
    ctx.setVariable("columns", sqlQueryRows.getMetaData().getColumns());
    List<List<Object>> rows = new ArrayList<>();
    ctx.setVariable("rows", rows);
    sqlQueryRows.getRows().forEach(rowDesc -> {
      List<Object> row = new ArrayList<>();
      sqlQueryRows.getMetaData().getColumns().forEach(columnDesc -> {
        if (rowDesc.isColumnNull(columnDesc)) {
          row.add(null);
        } else {
          row.add(rowDesc.getColumnValue(columnDesc));
        }
      });
      rows.add(row);
    });
    TemplateEngine templateEngine = new TemplateEngine();
    FileTemplateResolver resolver = new FileTemplateResolver();

    templateEngine.setTemplateResolver(resolver);
    IOUtils.write(templateEngine.process(templateFileName, ctx), outputStream);
  }

  /** Write rows from sql query to output stream
   * @param sqlQueryRows rows
   * @param outputStream stream to which are data write */
  public static void writeSqlQueryRowsToFreemarker(SQLQueryRows sqlQueryRows, InputStream template, OutputStream outputStream) throws IOException {
    Configuration cfg = new Configuration();
    cfg.setTemplateLoader(new TemplateLoader() {
      @Override @SuppressWarnings("RedundantThrowsDeclaration")
      public Object findTemplateSource(String s) throws IOException { return template; }
      @Override
      public long getLastModified(Object o) { return 0; }
      @Override @SuppressWarnings("RedundantThrowsDeclaration")
      public Reader getReader(Object o, String s) throws IOException { return new InputStreamReader(template); }
      @Override @SuppressWarnings("RedundantThrowsDeclaration")
      public void closeTemplateSource(Object o) throws IOException {}
    });
    cfg.setIncompatibleImprovements(new Version(2, 3, 20));
    cfg.setDefaultEncoding("UTF-8");
    cfg.setLocale(Locale.US);
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    Template freemarkerTemplate = cfg.getTemplate("template");
    Map<String, Object> input = new HashMap<>();
    input.put("sql", sqlQueryRows.getSQL());
    input.put("columns", sqlQueryRows.getMetaData().getColumns());
    List<List<Object>> rows = new ArrayList<>();
    input.put("rows", rows);
    sqlQueryRows.getRows().forEach(rowDesc -> {
      List<Object> row = new ArrayList<>();
      sqlQueryRows.getMetaData().getColumns().forEach(columnDesc -> {
        if (rowDesc.isColumnNull(columnDesc)) {
          row.add(null);
        } else {
          row.add(rowDesc.getColumnValue(columnDesc));
        }
      });
      rows.add(row);
    });

    input.put("sqlQueryRows", sqlQueryRows);
    try {
      freemarkerTemplate.process(input, new OutputStreamWriter(outputStream));
    } catch (TemplateException e) {
      LOG.error("Problem with transforming input stream.", e);
      ExceptionMessageFrmController.showException("Problem with transforming input stream.", e);
    }
  }
}
