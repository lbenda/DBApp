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

import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.AbstractFileProvider;
import org.apache.commons.vfs2.provider.FileProvider;

import java.util.Collection;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 23.10.15.
 * Manager in apache VFS2 for read the files from home directory which start with ~ */
public class TildaFileProvider extends AbstractFileProvider implements FileProvider {

  public TildaFileProvider() {
    super();
  }

  @Override
  public FileObject findFile(FileObject fileObject, String uri, FileSystemOptions fileSystemOptions) throws FileSystemException {
    String userHome = System.getProperty("user.home");
    if (uri.startsWith("~")) {
      uri = uri.replace("~://", "file://" + userHome + "/");
      uri = uri.replace("~", "file://" + userHome);
    } else {
      uri = "file://" + userHome + "/" + uri.replace("home://", "");
    }
    FileSystemManager fsManager= VFS.getManager();
    return fsManager.resolveFile(uri);
  }

  @Override
  public Collection<Capability> getCapabilities() {
    return null;
  }

}
