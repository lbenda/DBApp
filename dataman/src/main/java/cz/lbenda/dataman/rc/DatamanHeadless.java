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
package cz.lbenda.dataman.rc;

import cz.lbenda.dataman.db.DbConfig;
import cz.lbenda.dataman.db.ExportTableData;
import cz.lbenda.dataman.db.SQLQueryResult;
import cz.lbenda.dataman.db.sql.SQLSExecutor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 24.10.15.
 * Headless version of dataman */
public class DatamanHeadless {

  private static final Logger LOG = LoggerFactory.getLogger(DatamanHeadless.class);

  /** Append new option to Options object which is used for parse command line arguments
   * @param options object to which is append options */
  public static void setCommandLineOptions(Options options) {
    options.addOption("i", "script", true, "Input script which is executes.");
    options.addOption("is", "scripts", true, "Input scripts which is executes separated by comma");
    options.addOption("c", "config", true, "Name of configuration on which is executed script");
    options.addOption("o", "output", true, "Output file to which is data written");
    options.addOption("os", "outputs", true, "Output files to which is data written separated by comma. In order as input file.");
    options.addOption("f", "format", true, "Format which is used for formatting the returned data. CSV, SQL, TXT, XMLv1, XMLv2. Default is TXT.");
    options.addOption(null, "nos", false, "Flag which switch of write SQL header to output.");
  }

  private @Nonnull List<FileObject> scripts(CommandLine cmd) throws FileSystemException {
    return files(cmd, "i", "is");
  }

  private @Nonnull List<FileObject> outputs(CommandLine cmd) throws FileSystemException {
    return files(cmd, "o", "os");
  }

  private @Nonnull List<FileObject> files(CommandLine cmd, String singleFileOption, String multipleFileOption) throws FileSystemException {
    if (!cmd.hasOption(singleFileOption) && !cmd.hasOption(multipleFileOption)) { return Collections.emptyList(); }
    String script = cmd.getOptionValue(singleFileOption);
    FileSystemManager fsm = VFS.getManager();
    if (script != null) {
      return Collections.singletonList(fsm.resolveFile((FileObject) null, script));
    }
    String scripts = cmd.getOptionValue(multipleFileOption);
    String[] scrs = scripts.split(",");
    List<FileObject> result = new ArrayList<>();
    for (String scr : scrs) {
      result.add(fsm.resolveFile((FileObject) null, scr));
    }
    return result;
  }

  private @Nonnull ExportTableData.SpreadsheetFormat format(@Nonnull CommandLine cmd) {
    if (cmd.hasOption("f")) {
      return ExportTableData.SpreadsheetFormat.valueOf(cmd.getOptionValue("f").toUpperCase());
    }
    return ExportTableData.SpreadsheetFormat.TXT;
  }

  public void launch(@Nonnull CommandLine cmd) {
    DbConfig dbConfig = DbConfigFactory.getConfiguration(cmd.getOptionValue("c"));
    if (dbConfig == null) {
      LOG.error("Configuration with name " + cmd.getOptionValue("c") + " not exist.");
      System.exit(1);
    }

    try {
      List<FileObject> scripts = scripts(cmd);
      List<FileObject> outputs = outputs(cmd);
      if (scripts.size() == 0) {
        LOG.error("The scripts isn't defined");
        System.exit(3);
      } else {
        SQLSExecutorConsumerClass sqlEditorController = new SQLSExecutorConsumerClass(!cmd.hasOption("nos"), format(cmd));
        SQLSExecutor executor = new SQLSExecutor(dbConfig, sqlEditorController, null);
        dbConfig.getConnectionProvider().getConnection();
        int i = 0;
        for (FileObject file : scripts) {
          if (outputs.size() > i) { sqlEditorController.setOutputFile(outputs.get(i)); }
          else { sqlEditorController.setOutputFile(null); }
          i++;
          try {
            System.out.println("Script file: " + file.getName().getFriendlyURI());
            InputStream is = file.getContent().getInputStream();
            Reader reader = new InputStreamReader(is);
            char[] buff = new char[262144];
            int l;
            String rest = "";
            while ((l = reader.read(buff)) != -1) {
              String str = rest + new String(buff, 0, l);
              String[] strs = SQLSExecutor.splitSQLS(str);
              if (strs.length == 1) {
                rest = str;
              } else {
                rest = strs[strs.length - 1];
                if (!str.endsWith("\n") && rest.endsWith("\n")) {
                  rest = rest.substring(0, rest.length() - 1);
                  rest = str.substring(str.lastIndexOf(rest), str.length());
                }
                String[] sqls = new String[strs.length - 1];
                System.arraycopy(strs, 0, sqls, 0, strs.length - 1);
                executor.executeBlocking(sqls);
              }
            }
            executor.executeBlocking(SQLSExecutor.splitSQLS(rest));
          } catch (IOException e) {
            LOG.error("Problem with read script file", e);
            System.exit(4);
          }
        }
      }
    } catch (FileSystemException e) {
      LOG.error("Problem with read script data", e);
      System.exit(2);
    }
  }

  public static class SQLSExecutorConsumerClass implements SQLSExecutor.SQLSExecutorConsumer {
    private ExportTableData.SpreadsheetFormat format;
    private PrintStream printStream;
    private boolean writeSQLHeaders;

    public void setOutputFile(FileObject outputFile) {
      printStream = currentWriter(outputFile);
    }

    SQLSExecutorConsumerClass(boolean writeSQLHeaders, ExportTableData.SpreadsheetFormat format) {
      this.format = format;
      this.writeSQLHeaders = writeSQLHeaders;
    }
    private PrintStream currentWriter(FileObject outputFile) {
      if (outputFile == null) { return System.out; }
      try {
        if (!outputFile.isWriteable()) {
          LOG.error("The output file isn't writable", outputFile.getName());
          System.exit(10);
        }
         return new PrintStream(outputFile.getContent().getOutputStream(), true);
      } catch (IOException e) {
        LOG.error("The error when output file is open", e);
        System.exit(11);
        throw new RuntimeException("The error when output file is open", e);
      }
    }
    @Override
    public void addQueryResult(SQLQueryResult result) {
      if (writeSQLHeaders) {
        printStream.println("------");
        printStream.println(result.getSql());
        printStream.println("------");
      }
      if (!StringUtils.isBlank(result.getErrorMsg())) { LOG.error(result.getErrorMsg()); }
      else {
        if (result.getSqlQueryRows() != null) {
          try {
            StringWriter sw = new StringWriter();
            ExportTableData.writeSqlQueryRows(format, result.getSqlQueryRows(), null, sw);
            printStream.println(sw.toString());
          } catch (IOException e) {
           LOG.error("Error when result of table is write as CSV", e);
          }
        } else if (result.getAffectedRow() != null) {
          printStream.println("Affected rows <" + result.getAffectedRow() + ">");
        }
      }
    }
    @Override
    public boolean isStopOnFirstError() { return true; }
  }
}
