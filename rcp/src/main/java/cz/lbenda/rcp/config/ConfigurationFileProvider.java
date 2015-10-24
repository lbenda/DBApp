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
import org.apache.commons.vfs2.provider.UriParser;

import javax.annotation.Nonnull;
import java.util.Collection;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 23.10.15.
 * Manager in apache VFS2 for read and write configuration */
public class ConfigurationFileProvider extends AbstractFileProvider implements FileProvider, Comparable<Object> {

  private ConfigurationRW configurationRW;

  public ConfigurationFileProvider() {
    configurationRW = ConfigurationRW.getInstance();
  }

  @Override
  public FileObject findFile(FileObject fileObject, String uri, FileSystemOptions fileSystemOptions) throws FileSystemException {
    final StringBuilder buffer = new StringBuilder(uri);
    UriParser.extractScheme(uri, buffer);
    UriParser.fixSeparators(buffer);

    UriParser.normalisePath(buffer);
    final String path = buffer.toString();

    FileSystemManager fsManager= VFS.getManager();
    return fsManager.resolveFile("file://" + configurationRW.configPath(path));
  }

  @Override
  public Collection<Capability> getCapabilities() {
    return null;
  }

  @Override
  public int compareTo(@Nonnull Object o) {
    int h1 = hashCode();
    int h2 = o.hashCode();
    if (h1 < h2) { return -1; }
    if (h1 > h2) { return 1; }
    return 0;
  }
}
