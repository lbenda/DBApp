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
package cz.lbenda.dataman.db;

import cz.lbenda.common.AbstractHelper;
import cz.lbenda.rcp.SimpleLocalDateProperty;
import cz.lbenda.rcp.SimpleLocalDateTimeProperty;
import cz.lbenda.rcp.SimpleLocalTimeProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 13.9.15.
 * Description of row */
public class RowDesc implements Observable {

  public enum RowDescState {
    /** Row is newly added */
    NEW,
    /** Row loaded from database */
    LOADED,
    /** Removed for from database */
    REMOVED,
    /** Row is changed */
    CHANGED, ;
  }

  /** Array for calculate has code */
  private static int[] PRIMES = {17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229, 233, 239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293, 307, 311, 313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, 383, 389, 397, 401, 409, 419, 421, 431, 433, 439, 443, 449, 457, 461, 463, 467, 479, 487, 491, 499, 503, 509, 521, 523, 541, 547, 557, 563, 569, 571, 577, 587, 593, 599, 601, 607, 613, 617, 619, 631, 641, 643, 647, 653, 659, 661, 673, 677, 683, 691, 701, 709, 719, 727, 733, 739, 743, 751, 757, 761, 769, 773, 787, 797, 809, 811, 821, 823, 827, 829, 839, 853, 857, 859, 863, 877, 881, 883, 887, 907, 911, 919, 929, 937, 941, 947, 953, 967, 971, 977, 983, 991, 997};

  private List<InvalidationListener> invalidationListeners = new ArrayList<>();
  private Object id; public Object getId() { return id; } public void setId(Object id) { this.id = id; }
  private Object[] oldValues; public Object[] getOldValues() { return oldValues; } public void setOldValues(Object[] oldValues) { this.oldValues = oldValues; }
  private Object[] newValues; public Object[] getNewValues() { return newValues; }

  private RowDescState state;

  /** Row in state new */
  public static RowDesc createNewRow(SQLQueryMetaData metaData) {
    RowDesc result = new RowDesc();
    result.newValues = new Object[metaData.columnCount()];
    result.state = RowDescState.NEW;
    return result;
  }

  private RowDesc() {}

  public RowDesc(Object id, Object[] values, RowDescState state) {
    this.id = id;
    this.oldValues = values;
    this.newValues = values.clone();
    this.state = state;
  }

  public void setState(RowDescState state) {
    this.state = state;
    doInvalidation();
  }
  public RowDescState getState() {
    return state;
  }

  public void cancelChanges() {
    if (oldValues != null) {
      newValues = oldValues.clone();
      state = RowDescState.LOADED;
    } else {
      newValues = new Object[newValues.length];
      state = RowDescState.NEW;
    }
    doInvalidation();
  }

  /** The changes was saved */
  public void savedChanges() {
    oldValues = newValues.clone();
    state = RowDescState.LOADED;
    doInvalidation();
  }

  @Override
  public void addListener(InvalidationListener invalidationListener) { invalidationListeners.add(invalidationListener); }
  @Override
  public void removeListener(InvalidationListener invalidationListener) { invalidationListeners.remove(invalidationListener); }

  /** Call invalidation on all invalidation listener */
  public void doInvalidation() {
    invalidationListeners.forEach(listener -> listener.invalidated(this));
  }

  /** Return value from column */
  public Object getColumnValue(ColumnDesc column) {
    return getNewValues()[column.getPosition()];
  }
  /** Return value of column in string */
  public String getColumnValueStr(ColumnDesc column) {
    //noinspection unchecked
    return column.getStringConverter().toString(getNewValues()[column.getPosition()]);
  }

  /** Return set value for given column */
  public void setColumnValue(ColumnDesc column, Object value) {
    getNewValues()[column.getPosition()] = value;
    if (RowDescState.LOADED == state
        && !AbstractHelper.nullEquals(value, getOldValues()[column.getPosition()])) {
      this.setState(RowDescState.CHANGED);
    } else if (state == RowDescState.CHANGED) {
      boolean noChanged = true;
      for (int i = 0; i < getOldValues().length; i++) {
        noChanged = noChanged && AbstractHelper.nullEquals(getNewValues()[i], getOldValues()[i]);
      }
      if (noChanged) { setState(RowDescState.LOADED); }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) { return false; }
    if (!(o instanceof RowDesc)) { return false; }

    RowDesc sqr = (RowDesc) o;
    if (id != null) { return id.equals(sqr.getId()); }
    else if (sqr.getId() != null) { return false; }

    if (oldValues == null) { return sqr.getOldValues() == null; }
    else if (sqr.getOldValues() == null) { return false; }
    return Arrays.equals(oldValues, sqr.getOldValues());
  }

  @Override
  public int hashCode() {
    if (id != null) { return id.hashCode(); }
    if (oldValues == null) { return 0; }
    int result = 1;
    int primeI = 0;
    for (Object v : oldValues) {
      if (primeI >= PRIMES.length) { primeI = 0; }
      result *= PRIMES[primeI];
      if (v != null) { result += v.hashCode(); }
    }
    return result;
  }

  @Override
  @SuppressWarnings("CloneDoesntCallSuperClone")
  public RowDesc clone() {
    RowDesc result = new RowDesc(id, oldValues.clone(), state);
    return result;
  }

  @SuppressWarnings("unchecked")
  public ObservableValue observableValueForColumn(@Nonnull ColumnDesc columnDesc) {
    ObservableValue result;
    switch (columnDesc.getDataType()) {
      case BOOLEAN:
        result = new SimpleBooleanProperty(newValues[columnDesc.getPosition()], null);
        break;
      case INTEGER:
        // result = new javafx.beans.property.SimpleIntegerProperty(newValues[columnDesc.getPosition()], null);
        if (newValues[columnDesc.getPosition()] == null) {
          result = new SimpleStringProperty(null);
        } else {
          result = new SimpleStringProperty(String.valueOf(newValues[columnDesc.getPosition()]));
        }
        break;
      case DATE:
        result = new SimpleLocalDateProperty((java.sql.Date) newValues[columnDesc.getPosition()]);
        break;
      case TIMESTAMP:
        result = new SimpleLocalDateTimeProperty((Timestamp) newValues[columnDesc.getPosition()]);
        break;
      case TIME:
        result = new SimpleLocalTimeProperty((Time) newValues[columnDesc.getPosition()]);
        break;
      case STRING:
        result = new SimpleStringProperty((String) newValues[columnDesc.getPosition()]);
        break;
      default:
        result = new SimpleObjectProperty<>(newValues[columnDesc.getPosition()]);
        break;
    }
    result.addListener((observable, oldValue, newValue) -> {
      if (observable instanceof SimpleLocalDateProperty) {
        setColumnValue(columnDesc, ((SimpleLocalDateProperty) observable).getSQLDate());
      } else if (observable instanceof SimpleLocalDateTimeProperty) {
        setColumnValue(columnDesc, ((SimpleLocalDateTimeProperty) observable).getSQLTimestamp());
      } else if (observable instanceof SimpleLocalTimeProperty) {
        setColumnValue(columnDesc, ((SimpleLocalTimeProperty) observable).getSQLTime());
      } else if (columnDesc.getDataType() == ColumnDesc.ColumnType.INTEGER)  {
        if (StringUtils.isBlank(((SimpleStringProperty) newValue).getValue())) {
          setColumnValue(columnDesc, null);
        } else {
          setColumnValue(columnDesc, Integer.valueOf(((SimpleStringProperty) newValue).getValue()));
        }
      } else {
        setColumnValue(columnDesc, newValue);
      }
    });
    return result;
  }
}
