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
package cz.lbenda.rcp.config;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Class which read and write configuration data */
public class ConfigurationRW {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigurationRW.class);
  private static ConfigurationRW configurationRW;

  /** Create single instace of configuration RW
   * @param appId Identifier of application which is used for create configuration directory. The best way is use
   *              prefix same as in java package.
   * @param version When you want separate configuration for different version of application elsewhere can stay null */
  public static void createInstance(String appId, String version) {
    configurationRW = new ConfigurationRW(appId, version);
  }

  /** Return already created instance of configuration RW
   * @return already create instance of configuration which must be create by {@link ConfigurationRW#createInstance(String, String)}
   * @throws IllegalStateException when you want get instance of configuraiton RW before you create it
   * */
  public static ConfigurationRW getInstance() throws IllegalStateException {
    if (configurationRW == null) { throw new IllegalStateException("The instance isn't created yet."); }
    return configurationRW;
  }

  /** Identifier of application which is used for create configuration directory. The best way is use
   *              prefix same as in java package. */
  private String appId; public String getAppId() { return appId; }
  /** Version of application if user want have more version */
  private String version; public String vetVersion() { return version; }

  private ConfigurationRW(String appId, String version) {
    this.appId = appId;
    this.version = version;
  }

  /** Return content of configuration which name is given. The any configuration is saved as file, so the config
   * name is file name
   * @param configFile name of file
   * @return input stream if config file exist elsewhere return input stream which is empty */
  public InputStream readConfig(String configFile) {
    File userDir = userConfigDirectoryPath();
    if (userDir.exists()) {
      File file = new File(userDir, configFile);
      if (file.exists()) {
        try {
          return new FileInputStream(file);
        } catch (FileNotFoundException e) {
          LOG.error("Problem with read config file: " + configFile, e);
          throw new RuntimeException("Problem with read config file: " + configFile, e);
        }
      }
    }
    return new ByteArrayInputStream(new byte[0]);
  }

  /** Return content of configuration which name is given. The any configuration is saved as file, so the config
   * name is file name
   * @param configFile name of file
   * @return configuration or NULL when config file isn't openable or is empty */
  public String readConfigAsString(String configFile) {
    try (InputStream is = readConfig(configFile)) {
      String result = IOUtils.toString(is);
      if (result == null || "".equals(result)) { return null; }
      return result;
    } catch (IOException e) {
      LOG.error("The config file " + configFile + " is unreadable: " + e.toString(), e);
    }
    return null;
  }

  /** Write stream with configuration to file
   * @param configFile name of configuration wile where is data write
   * @return stream to which user can write file data
   * @throws IOException problem with creating output stream
   */
  public OutputStream writeConfig(String configFile) throws IOException {
    File userDir = userConfigDirectoryPath();
    if (!userDir.exists()) { userDir.mkdirs(); }
    File file = new File(userDir, configFile);
    return new FileOutputStream(file);
  }

  /** Write stream with configuration to file
   * @param configFile name of configuration wile where is data write
   * @param content content which will be saved
   * @return stream to which user can write file data
   * @throws RuntimeException hold the IOException
   */
  public void writeConfig(String configFile, String content) throws RuntimeException {
    try (OutputStream os = writeConfig(configFile)) {
      IOUtils.write(content, os);
    } catch (IOException e) {
      throw new RuntimeException("Problem with write configuration to file: " + configFile, e);
    }
  }

  /** Return last part of config directory path */
  private String lastConfigDirectory() {
    return appId + (version != null ? "_" + version : "");
  }

  private File userConfigDirectoryPath() {
    // Windows
    String dataFolder = System.getenv("LOCALAPPDATA");
    if (dataFolder == null) { dataFolder = System.getenv("APPDATA"); }
    if (dataFolder != null) {
      File parent = new File(dataFolder);
      if (parent.exists()) { new File(parent, lastConfigDirectory()); }
    }

    // Mac
    File home = SystemUtils.getUserHome();
    if (SystemUtils.IS_OS_MAC) {
      File f = new File(home, "Library/Application Support/");
      if (f.exists()) { return new File(f, lastConfigDirectory()); }
    }

    // Linux/*NIX
    File configDirectory = new File(home, ".config");
    if (configDirectory.exists()) { return new File(configDirectory, lastConfigDirectory()); }
    File localDirectory = new File(home, ".local");
    if (localDirectory.exists()) { return new File(localDirectory, lastConfigDirectory()); }
    return new File(home, "." + lastConfigDirectory());
  }
}
