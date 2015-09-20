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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 17.9.15.
 * Obfuscate configurations */
public class Obfuscate {
  private static Logger LOG = LoggerFactory.getLogger(Obfuscate.class);

  private static final char[] PASSWORD = "[en/*fldsgbnlSHUJdl589sgm".toCharArray();
  private static final byte SALT_LENGTH = 8;

  private static byte[] createSalt() {
    byte[] result = new byte[SALT_LENGTH];
    (new Random()).nextBytes(result);
    return result;
  }

  public static String obfuscate(String property) {
    try {
      SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
      SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
      Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
      byte[] salt = createSalt();
      pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(salt, 20));

      byte[] crypt = pbeCipher.doFinal(property.getBytes("UTF-8"));
      byte[] saltedCrypt = new byte[salt.length + crypt.length];
      System.arraycopy(salt, 0, saltedCrypt, 0, salt.length);
      System.arraycopy(crypt, 0, saltedCrypt, salt.length, crypt.length);

      return base64Encode(saltedCrypt);
    } catch (GeneralSecurityException | UnsupportedEncodingException e) {
      LOG.error("Problem with obfuscate", e);
      return property;
    }
  }

  private static String base64Encode(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  public static String deobfuscate(String property)  {
    try {
      byte[] saltedCrypt = base64Decode(property);
      byte[] salt = new byte[SALT_LENGTH];
      byte[] crypt = new byte[saltedCrypt.length - SALT_LENGTH];

      System.arraycopy(saltedCrypt, 0, salt, 0, SALT_LENGTH);
      System.arraycopy(saltedCrypt, SALT_LENGTH, crypt, 0, saltedCrypt.length - SALT_LENGTH);

      SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
      SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
      Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
      pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(salt, 20));
      return new String(pbeCipher.doFinal(crypt), "UTF-8");
    } catch (GeneralSecurityException | IOException e) {
      LOG.error("Problem with de-obfuscate", e);
      return property;
    }
  }

  private static byte[] base64Decode(String property) {
    return Base64.getDecoder().decode(property);
  }
}
