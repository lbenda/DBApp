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

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 17.9.15.
 * Obfuscate configurations */
public class Obfuscate {
  private static Logger LOG = LoggerFactory.getLogger(Obfuscate.class);

  private static final char[] PASSWORD = "[en/*fldsgbnlSHUJdl589sgm".toCharArray();
  private static final byte[] SALT = {
      (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
      (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
  };

  public static String obfuscate(String property) {
    try {
      SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
      SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
      Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
      pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
      return base64Encode(pbeCipher.doFinal(property.getBytes("UTF-8")));
    } catch (GeneralSecurityException | UnsupportedEncodingException e) {
      LOG.error("Problem with obfuscate", e);
      return property;
    }
  }

  private static String base64Encode(byte[] bytes) {
    // NB: This class is internal, and you probably should use another impl
    return new BASE64Encoder().encode(bytes);
  }

  public static String deobfuscate(String property)  {
    try {
      SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
      SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
      Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
      pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
      return new String(pbeCipher.doFinal(base64Decode(property)), "UTF-8");
    } catch (GeneralSecurityException | IOException e) {
      LOG.error("Problem with deobfuscate", e);
      return property;
    }
  }

  private static byte[] base64Decode(String property) throws IOException {
    // NB: This class is internal, and you probably should use another impl
    return new BASE64Decoder().decodeBuffer(property);
  }
}
