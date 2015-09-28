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
package cz.lbenda.dataman.db.dialect;

import cz.lbenda.common.BinaryData;
import cz.lbenda.common.StringConverters;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 27.9.15. */
public enum ColumnType {
  SHORT(Short.class, StringConverters.SHORT_CONVERTER),
  BYTE(Byte.class, StringConverters.BYTE_CONVERTER),
  INTEGER(Integer.class, StringConverters.INT_CONVERTER),
  LONG(Long.class, StringConverters.LONG_CONVERTER),
  BYTE_ARRAY(Byte[].class, StringConverters.BINARYDATA_CONVERTER),
  UUID(java.util.UUID.class, StringConverters.UUID_CONVERTER),
  ARRAY(Array.class, StringConverters.OBJECTARRAY_CONVERTER),
  BIT(Byte.class, StringConverters.BYTE_CONVERTER),
  BIT_ARRAY(BinaryData.class, StringConverters.BITARRAY_CONVERTER),

  FLOAT(Float.class, StringConverters.FLOAT_CONVERTER),
  DOUBLE(Double.class, StringConverters.DOUBLE_CONVERTER),
  DECIMAL(BigDecimal.class, StringConverters.DECIMAL_CONVERTER),

  BOOLEAN(Boolean.class, StringConverters.BOOLEAN_CONVERTER),
  STRING(String.class, StringConverters.STRING_CONVERTER),

  DATE(Date.class, StringConverters.SQL_DATE_CONVERTER),
  TIMESTAMP(Timestamp.class, StringConverters.SQL_TIMESTAMP_CONVERTER),
  TIME(Time.class, StringConverters.SQL_TIME_CONVERTER),

  BLOB(BinaryData.class, StringConverters.BINARYDATA_CONVERTER),
  CLOB(BinaryData.class, StringConverters.BINARYDATA_CONVERTER),

  OBJECT(Object.class, StringConverters.OBJECT_CONVERTER), ;

  private final Class clazz;
  private final StringConverter converter;

  ColumnType(Class clazz, StringConverter converter) {
    this.clazz = clazz;
    this.converter = converter;
  }

  @SuppressWarnings("unused")
  public Class getJavaClass() {
    return clazz;
  }
  public StringConverter getConverter() {
    return converter;
  }
}
