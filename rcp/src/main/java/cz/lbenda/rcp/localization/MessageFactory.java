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
package cz.lbenda.rcp.localization;

import cz.lbenda.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.ResourceBundle;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Factory which return messages for application */
public class MessageFactory {

  private static final Logger LOG = LoggerFactory.getLogger(MessageFactory.class);
  private static MessageFactory instance;

  public static void createInstance(ResourceBundle resourceBundle) {
    instance = new MessageFactory(resourceBundle);
  }

  public static MessageFactory getInstance() {
    if (instance == null) {
      throw new IllegalStateException("The instance must be created by method createInstance before");
    }
    return instance;
  }

  /** Initialize message classes in given base package */
  public void initializePackage(String basePackage) {
    new Thread(() -> {
      List<String> classes = ClassLoaderHelper.classInPackage(basePackage, getClass().getClassLoader());
      classes.parallelStream().forEach(cName -> {
        try {
          getInstance().initializeMessages(getClass().getClassLoader().loadClass(cName));
        } catch (ClassNotFoundException e) {
          /* Ignore this problem. It's normal because there ins't all dependencies. */
        }
      });
    }).start();
  }

  /** Initialize given object or class with localized messages
   * @param o object which is initialized */
  @SuppressWarnings({"unused", "UnusedAssignment"})
  public <T> void initializeMessages(T o)  {
    if (o == null) { return; }
    Class clazz;
    if (o instanceof Class) { clazz = (Class) o; }
    else { clazz = o.getClass(); }
    do {
      for (Field field : clazz.getDeclaredFields()) {
        Message message = field.getDeclaredAnnotation(Message.class);
        if (message != null) {
          String mess = getMessage(clazz.getName() + "." + field.getName(), null);
          if (mess != null) {
            field.setAccessible(true);
            try {
              field.set(o, mess);
            } catch (IllegalAccessException e) {
              LOG.error("Problem with set message for field: " + o.getClass().getName() + "." + field.getName(), e);
            }
          }
        }
      }
      clazz = clazz.getSuperclass();
    } while (clazz != null && !Object.class.equals(clazz));
  }

  private ResourceBundle messages;

  private MessageFactory(ResourceBundle messages) {
    this.messages = messages;
  }

  /** Return message by key
   * @param key return message by key. If isn't defined then return ??? key ???
   * @return localized message */
  public String getMessage(String key) { return getMessage(key, "??? " + key + " ???"); }

  /** Return message by key
   * @param key return message by key. If isn't defined then return ??? key ???
   * @return localized message */
  public String getMessage(String key, String def) {
    String result = null;
    if (messages != null) {
      if (messages.containsKey(key)) {
        result = messages.getString(key);
      } else {
        if (LOG.isDebugEnabled() && Constants.IS_IN_DEVELOP_MODE) {
          LOG.debug("The message with key: '" + key + "' wasn't found in messages.");
        }
      }
    }
    return result == null ? def : result;
  }

  /** Return message which is defined in resource bundle for given ID or return default message or ??? message.id() ???
   * @param message return message by key. If isn't defined then return ??? message.id() ???
   * @return localized message */
  public String getMessage(Message message) {
    String result = null;
    if (messages != null) {
      if (messages.containsKey(message.id())) {
        result = messages.getString(message.id());
      }
    }
    return result == null ? (message.msg() == null ? "??? " + message.id() + " ???" : message.msg()) : result;
  }
}
