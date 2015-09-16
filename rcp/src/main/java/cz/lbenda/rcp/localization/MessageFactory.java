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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Factory which return messages for application */
public class MessageFactory {

  private static final Logger LOG = LoggerFactory.getLogger(MessageFactory.class);
  private static MessageFactory instance;

  public static void createInstanance(ResourceBundle resourceBundle) {
    instance = new MessageFactory(resourceBundle);
  }

  public static MessageFactory getInstance() {
    if (instance == null) {
      throw new IllegalStateException("The instance must be created by method createInstance before");
    }
    return instance;
  }

  /** Initialize given object with localized messages
   * @param o object which is initialized */
  public static void initializeMessages(Object o)  {
    /*
    if (o == null) { return; }
    Class clazz = o.getClass();
    do {
      for (Field field : clazz.getDeclaredFields()) {
        Message message = field.getDeclaredAnnotation(Message.class);
        if (message != null) {
          field.setAccessible(true);
          try {
            field.set(o, message.value());
          } catch (IllegalAccessException e) {
            LOG.error("Problem with set message for field: " + o.getClass().getName() + "." + field.getName(), e);
          }
        }
      }
      clazz = clazz.getSuperclass();
    } while (clazz != null && !Object.class.equals(clazz));
    */
  }

  private ResourceBundle messages;

  private MessageFactory(ResourceBundle messages) {
    this.messages = messages;
  }

  /** Return message by key
   * @param key return message by key. If isn't defined then return ??? key ???
   * @return localized message */
  public String getMessage(String key) {
    String result = null;
    if (messages != null) {
      if (messages.containsKey(key)) {
        result = messages.getString(key);
      } else {
        LOG.warn("The message with key: '" + key + "' wasn't found in messages.");
      }
    }
    return result == null ? "??? " + key + " ???" : result;
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

  /** Return title for given category */
  public String actionCategoryTitle(String category) {
    return getMessage("ribbonCategory_" + category);
  }
}
