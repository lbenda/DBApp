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

  private List<FileObject> scripts(CommandLine cmd) throws FileSystemException {
    String script = cmd.getOptionValue("i");
    FileSystemManager fsm = VFS.getManager();
    if (script != null) {
      return Collections.singletonList(fsm.resolveFile((FileObject) null, script));
    }
    String scripts = cmd.getOptionValue("s");
    String[] scrs = scripts.split(",");
    List<FileObject> result = new ArrayList<>();
    for (String scr : scrs) {
      result.add(fsm.resolveFile((FileObject) null, scr));
    }
    return result;
  }

  public void launch(@Nonnull CommandLine cmd) {
    DbConfig dbConfig = DbConfigFactory.getConfiguration(cmd.getOptionValue("c"));
    if (dbConfig == null) {
      LOG.error("Configuration with name " + cmd.getOptionValue("c") + " not exist.");
      System.exit(1);
    }

    try {
      List<FileObject> scripts = scripts(cmd);
      if (scripts.size() == 0) {
        LOG.error("The scripts isn't defined");
        System.exit(3);
      } else {
        SQLSExecutorConsumerClass sqlEditorController = new SQLSExecutorConsumerClass();
        SQLSExecutor executor = new SQLSExecutor(dbConfig, sqlEditorController, null);
        dbConfig.getConnectionProvider().getConnection();
        scripts.forEach(file -> {
          try {
            System.out.println("Script file: " + file.getName().getFriendlyURI());
            InputStream is = file.getContent().getInputStream();
            Reader reader = new InputStreamReader(is);
            char[] buff = new char[262144];
            int l = 0;
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
        });
      }
    } catch (FileSystemException e) {
      LOG.error("Problem with read script data", e);
      System.exit(2);
    }
  }

  public static class SQLSExecutorConsumerClass implements SQLSExecutor.SQLSExecutorConsumer {
    @Override
    public void addQueryResult(SQLQueryResult result) {
      System.out.println("------");
      System.out.println(result.getSql());
      System.out.println("------");
      if (!StringUtils.isBlank(result.getErrorMsg())) { LOG.error(result.getErrorMsg()); }
      else {
        if (result.getSqlQueryRows() != null) {
          try {
            StringWriter sw = new StringWriter();
            ExportTableData.writeSqlQueryRowsToCSV(result.getSqlQueryRows(), sw);
            System.out.println(sw.toString());
          } catch (IOException e) {
           LOG.error("Error when result of table is write as CSV", e);
          }
        } else if (result.getAffectedRow() != null) {
          System.out.println("Affected rows <" + result.getAffectedRow() + ">");
        }
      }
    }
    @Override
    public boolean isStopOnFirstError() { return true; }
  }
}
