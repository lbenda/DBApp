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

import javafx.util.StringConverter;

import java.util.function.Supplier;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 2.10.15.
 * String converter in one package with value which hold */
@SuppressWarnings("unused")
public class StringConverterHolder<T> {

  private StringConverter<T> stringConverter;
  public void setStringConverter(StringConverter<T> stringConverter) { this.stringConverter = stringConverter; }
  public StringConverter<T> getStringConverter() { return stringConverter; }

  private T item;
  public void setItem(T item) { this.item = item; }
  public T getItem() { return item; }

  private ToStringer<T> toStringer;
  public void setToStringer(ToStringer<T> toStringer) { this.toStringer = toStringer; }
  public ToStringer<T> getToStringer() { return toStringer; }

  private Supplier<String> stringSupplier;
  public void setStringSupplier(Supplier<String> stringSupplier) { this.stringSupplier = stringSupplier; }
  public Supplier<String> getStringSupplier() { return this.stringSupplier; }

  public StringConverterHolder() {}

  public StringConverterHolder(T item, StringConverter<T> stringConverter) {
    this.item = item;
    this.stringConverter = stringConverter;
  }

  public StringConverterHolder(T item, ToStringer<T> toStringer) {
    this.item = item;
    this.toStringer = toStringer;
  }

  public StringConverterHolder(T item, Supplier<String> stringSupplier) {
    this.item = item;
    this.stringSupplier = stringSupplier;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof StringConverterHolder && AbstractHelper.nullEquals(item, ((StringConverterHolder) o).getItem());
  }

  @Override
  public int hashCode() {
    if (item == null) { return 0; }
    return item.hashCode();
  }

  @Override
  public String toString() {
    if (stringConverter == null && toStringer == null && stringSupplier == null) { return String.valueOf(item); }
    if (toStringer != null) { return toStringer.toString(item); }
    if (stringSupplier != null) { return stringSupplier.get(); }
    return stringConverter.toString(item);
  }
}
