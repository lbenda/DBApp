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
package cz.lbenda.dbapp.rc;

// import org.netbeans.Main;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class NbpStarter {

  private final static String WORKDIR = "/home/benzin/work/java/DBApp/DBApp-RC/target/";
  private final static String DEPLOY = WORKDIR + "dbapp/";

  public static void main(String[] pArgs) throws Exception {
    System.setProperty("netbeans.logger.console", "true"); // for logging on the console
    System.setProperty("netbeans.user", WORKDIR + "userdir"); // settings are stored here
    System.setProperty("netbeans.home", DEPLOY + "platform11"); // the netbeans cluster.

    System.setProperty("sun.awt.keepWorkingSetOnMinimize", "true"); // maven set this per default on starting

    String[] fixedArgs = {
        "--branding", "myappname"
    };

    LinkedList list = new LinkedList();
    list.addAll(Arrays.asList(fixedArgs));
    list.addAll(Arrays.asList(pArgs));

    // cleanup usercache (otherwise problems arise. cache seems to be not written correctly).
    _deleteUserCache();

   // Main.main(_listToArray(list));
  }

  private static String[] _listToArray(List pList) {
    return (String[]) pList.toArray(new String[pList.size()]);
  }

  private static void _deleteUserCache() {
    _delete(new File(WORKDIR + "userdir/var/cache"));
  }

  private static boolean _delete(File pPath) {
    if (pPath.exists()) {
      File[] files = pPath.listFiles();
      for (File file : files) {
        if (file.isDirectory()) { _delete(file); }
        else {
          //noinspection ResultOfMethodCallIgnored
          file.delete();
        }
      }
    }
    return (pPath.delete());
  }

}