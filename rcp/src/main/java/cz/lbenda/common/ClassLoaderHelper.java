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

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 4.10.15.
 * Helper for class loader */
public class ClassLoaderHelper {

  public static <T> Class<T> getClassFromLibs(String className, List<String> libs, boolean useSystemClassPath) throws ClassNotFoundException {
    ClassLoader clr = createClassLoader(libs, useSystemClassPath);
    //noinspection unchecked
    return (Class<T>) clr.loadClass(className);
  }

  public static ClassLoader createClassLoader(List<String> libs, boolean useSystemClassPath) {
    List<URL> urls = new ArrayList<>(libs.size());
    libs.forEach(lib -> {
      try {
        urls.add((new File(lib)).toURI().toURL());
      } catch (MalformedURLException e) {
        throw new RuntimeException("The file wasn't readable: " + lib, e);
      }
    });

    URLClassLoader urlCl;
    if (useSystemClassPath) {
      urlCl = new URLClassLoader(urls.toArray(new URL[urls.size()]), System.class.getClassLoader());
    } else { urlCl = new URLClassLoader(urls.toArray(new URL[urls.size()])); }
    return urlCl;
  }

  @SuppressWarnings("unchecked")
  public static List<String> instancesOfClass(Class clazz, List<String> libs, boolean abstractClass, boolean interf) {
    List<String> classNames = new ArrayList<>();
    ClassLoader clr = createClassLoader(libs, false);
    List<String> result = new ArrayList<>();
    libs.forEach(lib -> {
      try (ZipFile file = new ZipFile(lib)) {
        file.stream().forEach(entry -> {
          if (entry.getName().equals("META-INF/services/" + clazz.getName())) {
            try {
              String string = IOUtils.toString(file.getInputStream(entry));
              String[] lines = string.split("\n");
              for (String line : lines) { result.add(line.trim()); }
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          } else if (entry.getName().endsWith(".class")) {
            String className = entry.getName().substring(0, entry.getName().length() - 6).replace("/", ".");
            classNames.add(className);
          }
        });
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    classNames.forEach(cc -> {
      try {
        Class cla = clr.loadClass(cc);
        if ((interf || !cla.isInterface())
            && (abstractClass || !Modifier.isAbstract(cla.getModifiers()))
            && clazz.isAssignableFrom(cla)) {
          if (!result.contains(cc)) {
            result.add(cc);
          }
        }
      } catch (ClassNotFoundException | NoClassDefFoundError e) { /* It's common to try to create class which haven't class which need*/ }
    });
    return result;
  }

  private static List<File> subClasses(File file) {
    if (file != null) {
      if (file.isDirectory()) {
        List<File> result = new ArrayList<>();
        //noinspection ConstantConditions
        for (File f : file.listFiles()) {
          result.addAll(subClasses(f));
        }
        return result;
      } else {
        if (file.getName().endsWith(".class")) {
          //noinspection ArraysAsListWithZeroOrOneArgument
          return Arrays.asList(file);
        }
      }
    }
    return Collections.emptyList();
  }

  /** Stream of class which is in given packages
   * @param basePackage base package
   * @param classLoader class loader where is classes found
   * @return stream of class names */
  public static List<String> classInPackage(String basePackage, ClassLoader classLoader) {
    List<String> result = new ArrayList<>();
    try {
      for (Enumeration<URL> resources = classLoader.getResources(basePackage.replace(".", "/")); resources.hasMoreElements(); ) {
        URL url = resources.nextElement();
        if (String.valueOf(url).startsWith("file:")) {
          File file = new File(url.getFile());
          int prefixLength = url.getFile().length() - basePackage.length();
          List<File> files = subClasses(file);
          files.stream().forEach(f -> {
            String fName = f.getAbsolutePath();
            result.add(fName.substring(prefixLength, fName.length() - 6).replace("/", "."));
          });
        } else {
          URLConnection urlCon = url.openConnection();
          if (urlCon instanceof JarURLConnection) {
            JarURLConnection jarURLConnection = (JarURLConnection) urlCon;
            try (JarFile jar = jarURLConnection.getJarFile()) {
              jar.stream().forEach(entry -> {
                String entryName = entry.getName();
                if (entryName.endsWith(".class") && entryName.startsWith(basePackage)) {
                  result.add(entryName.substring(0, entryName.length() - 6));
                }
              });
            }
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return result;
  }
}
