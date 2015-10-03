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

import cz.lbenda.common.*;
import cz.lbenda.dataman.db.dialect.ColumnType;
import cz.lbenda.rcp.SimpleLocalDateProperty;
import cz.lbenda.rcp.SimpleLocalDateTimeProperty;
import cz.lbenda.rcp.SimpleLocalTimeProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.SingleSelectionModel;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 13.9.15.
 * Description of row */
public class RowDesc implements Observable {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(RowDesc.class);

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

  private Object[] oldValues;
  private Object[] newValues;


  private RowDescState state;

  /** Row in state new */
  public static RowDesc createNewRow(SQLQueryMetaData metaData) {
    RowDesc result = new RowDesc(metaData.columnCount());
    result.state = RowDescState.NEW;
    return result;
  }
  /** Row in state new */
  public static RowDesc createNewRow(SQLQueryMetaData metaData, RowDescState state) {
    RowDesc result = new RowDesc(metaData.columnCount());
    result.state = state;
    return result;
  }

  private RowDesc(int columnCount) {
    newValues = new Object[columnCount];
    oldValues = new Object[columnCount];
  }

  RowDesc(Object id, Object[] values, RowDescState state) {
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
  @SuppressWarnings("unchecked")
  public <T> T getColumnValue(ColumnDesc column) {
    return (T) newValues[column.getPosition() - 1];
  }
  /** Return value of column in string */
  @SuppressWarnings("unchecked")
  public String getColumnValueStr(ColumnDesc column) {
    return column.getStringConverter().toString(newValues[column.getPosition() - 1]);
  }

  /** Set value for both rows - old and new */
  public <T> void setInitialColumnValue(ColumnDesc column, T value) {
    Object v = value;
    if (column.getDataType() == ColumnType.BLOB) {
      v = new BlobBinaryData(column.toString(), (Blob) value);
    } else if (column.getDataType() == ColumnType.CLOB) {
      v = new ClobBinaryData(column.toString(), (Clob) value);
    } else if (column.getDataType() == ColumnType.BYTE_ARRAY) {
      v = new ByteArrayBinaryData(column.toString(), (byte[]) value);
    } else if (column.getDataType() == ColumnType.BIT_ARRAY) {
      v = new BitArrayBinaryData(column.toString(), (byte[]) value);
    }
    oldValues[column.getPosition() - 1] = v;
    newValues[column.getPosition() - 1] = v;
  }

  /** Load initial column value from rs */
  public void loadInitialColumnValue(ColumnDesc columnDesc, ResultSet rs) throws SQLException {
    Object val;
    if (columnDesc.getDataType() == ColumnType.BIT) {
      val = rs.getBoolean(columnDesc.getPosition());
      if (Boolean.TRUE.equals(val)) { val = (byte) 1; }
      else if (Boolean.FALSE.equals(val)) { val = (byte) 0; }
    }
    else if (columnDesc.getDataType() == ColumnType.BIT_ARRAY) { val = rs.getBytes(columnDesc.getPosition()); }
    else { val = rs.getObject(columnDesc.getPosition()); }
    setInitialColumnValue(columnDesc, val);
  }

  /** Return initial value of column */
  @SuppressWarnings("unchecked")
  public <T> T getInitialColumnValue(ColumnDesc column) {
    return (T) oldValues[column.getPosition() - 1];
  }

  /** Return set value for given column */
  public <T> void setColumnValue(ColumnDesc column, T value) {
    if (value instanceof SingleSelectionModel) {
      throw new ClassCastException("The value of column can't be selection model type.");
    }
    newValues[column.getPosition() - 1] = value;

    if (RowDescState.LOADED == state
        && !AbstractHelper.nullEquals(value, oldValues[column.getPosition() - 1])) {
      this.setState(RowDescState.CHANGED);
    } else if (state == RowDescState.CHANGED) {
      boolean noChanged = true;
      for (int i = 0; i < oldValues.length; i++) {
        noChanged = noChanged && AbstractHelper.nullEquals(newValues[i], oldValues[i]);
      }
      if (noChanged) { setState(RowDescState.LOADED); }
    }
  }

  /** True if value in column is NULL */
  public boolean isColumnNull(ColumnDesc column) {
    Object o = getColumnValue(column);
    return o == null || o instanceof BinaryData && ((BinaryData) o).isNull();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) { return false; }
    if (!(o instanceof RowDesc)) { return false; }

    RowDesc sqr = (RowDesc) o;
    if (id != null) { return id.equals(sqr.getId()); }
    else if (sqr.getId() != null) { return false; }

    if (oldValues == null) {
      return sqr.oldValues == null; }
    else if (sqr.oldValues == null) { return false; }
    return Arrays.equals(oldValues, sqr.oldValues);
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
        result = new SimpleBooleanProperty(getColumnValue(columnDesc), null);
        ((SimpleBooleanProperty) result).setValue(getColumnValue(columnDesc));
        break;
      case BYTE:
      case SHORT:
      case LONG:
      case INTEGER:
      case FLOAT:
      case DOUBLE:
      case DECIMAL:
        if (newValues[columnDesc.getPosition() - 1] == null) {
          result = new SimpleObjectProperty<>();//new SimpleStringProperty(null);
        } else {
          result = new SimpleObjectProperty<>(newValues[columnDesc.getPosition() - 1]);
          // new SimpleStringProperty(String.valueOf(newValues[columnDesc.getPosition()]));
        }
        break;
      case DATE:
        result = new SimpleLocalDateProperty((java.sql.Date) newValues[columnDesc.getPosition() - 1]);
        break;
      case TIMESTAMP:
        result = new SimpleLocalDateTimeProperty((Timestamp) newValues[columnDesc.getPosition() - 1]);
        break;
      case TIME:
        result = new SimpleLocalTimeProperty((Time) newValues[columnDesc.getPosition() - 1]);
        break;
      case STRING:
        result = new SimpleStringProperty((String) newValues[columnDesc.getPosition() - 1]);
        break;
      case BYTE_ARRAY:
      case CLOB:
      case BLOB:
        result = new SimpleObjectProperty<>(getColumnValue(columnDesc));
        break;
      default:
        result = new SimpleObjectProperty<>(newValues[columnDesc.getPosition() - 1]);
        break;
    }
    result.addListener((observable, oldValue, newValue) -> {
      if (newValue instanceof  SingleSelectionModel) {
        throw new ClassCastException("The selection model isn't right new value for row");
      }

      if (observable instanceof SimpleLocalDateProperty) {
        setColumnValue(columnDesc, ((SimpleLocalDateProperty) observable).getSQLDate());
      } else if (observable instanceof SimpleLocalDateTimeProperty) {
        setColumnValue(columnDesc, ((SimpleLocalDateTimeProperty) observable).getSQLTimestamp());
      } else if (observable instanceof SimpleLocalTimeProperty) {
        setColumnValue(columnDesc, ((SimpleLocalTimeProperty) observable).getSQLTime());
      } else if (columnDesc.getDataType() == ColumnType.SHORT
          || columnDesc.getDataType() == ColumnType.BYTE
          || columnDesc.getDataType() == ColumnType.INTEGER
          || columnDesc.getDataType() == ColumnType.LONG
          || columnDesc.getDataType() == ColumnType.FLOAT
          || columnDesc.getDataType() == ColumnType.DOUBLE
          || columnDesc.getDataType() == ColumnType.DECIMAL) {
        Object nVal;
        if (newValue == null) { nVal = null; }
        else if (newValue instanceof String) {
          if (StringUtils.isBlank((String) newValue)) {
            nVal = null;
          } else {
            nVal = columnDesc.getStringConverter().fromString((String) newValue);
          }
        } else if (newValue instanceof StringProperty) {
          if (StringUtils.isBlank(((StringProperty) newValue).getValue())) {
            nVal = null;
          } else {
            nVal = columnDesc.getStringConverter().fromString(((StringProperty) newValue).getValue());
          }
        } else { nVal = newValue; }
        setColumnValue(columnDesc, nVal);
      } else if (columnDesc.getDataType() == ColumnType.BYTE_ARRAY
          || columnDesc.getDataType() == ColumnType.BLOB
          || columnDesc.getDataType() == ColumnType.CLOB) {
        setColumnValue(columnDesc, newValue);
      } else {
        setColumnValue(columnDesc, newValue);
      }
    });
    return result;
  }

  /** return true if value in column was changed */
  public boolean isColumnChanged(ColumnDesc columnDesc) {
    return !AbstractHelper.nullEquals(oldValues[columnDesc.getPosition() - 1], newValues[columnDesc.getPosition() - 1]);
  }

  /** Insert into prepared statement initial value from current column
   * @param columnDesc column which values is inserted
   * @param ps prepared statement to which is data write
   * @param position position where are values inserted
   * @throws SQLException prepare statement can throw exception
   */
  public final void putInitialValueToPS(ColumnDesc columnDesc, PreparedStatement ps, int position) throws SQLException {
    putToPS(columnDesc, getInitialColumnValue(columnDesc), ps, position);
  }

  /** Insert into prepared statement value from current column
   * @param columnDesc column which values is inserted
   * @param ps prepared statement to which is data write
   * @param position position where are values inserted
   * @throws SQLException prepare statement can throw exception
   */
  public final void putValueToPS(ColumnDesc columnDesc, PreparedStatement ps, int position) throws SQLException {
    putToPS(columnDesc, getColumnValue(columnDesc), ps, position);
  }

  @SuppressWarnings("ConstantConditions")
  private <T> void putToPS(ColumnDesc columnDesc, T value, PreparedStatement ps, int position) throws SQLException {
    if (value == null) {
      ps.setObject(position, null);
      return;
    }
    BinaryData bd = value instanceof  BinaryData ? (BinaryData) value : null;
    switch (columnDesc.getDataType()) {
      case STRING: ps.setString(position, (String) value); break;
      case BOOLEAN: ps.setBoolean(position, (Boolean) value); break;
      case TIMESTAMP: ps.setTimestamp(position, (Timestamp) value); break;
      case DATE: ps.setDate(position, (Date) value); break;
      case TIME: ps.setTime(position, (Time) value); break;
      case BYTE: ps.setByte(position, (Byte) value); break;
      case SHORT: ps.setShort(position, (Short) value); break;
      case INTEGER: ps.setInt(position, (Integer) value); break;
      case LONG: ps.setLong(position, (Long) value); break;
      case FLOAT: ps.setFloat(position, (Float) value); break;
      case DOUBLE: ps.setDouble(position, (Double) value); break;
      case DECIMAL: ps.setBigDecimal(position, (BigDecimal) value); break;
      case UUID: ps.setBytes(position, AbstractHelper.uuidToByteArray((UUID) value)); break;
      case ARRAY:
        throw new UnsupportedOperationException("The saving changes in ARRAY isn't supported.");
        // ps.setArray(position, (Array) value); break; // FIXME the value isn't in type java.sql.Array
      case BYTE_ARRAY:
        if (bd == null || bd.isNull()) { ps.setBytes(position, null); }
        else {
          try {
            ps.setBytes(position, IOUtils.toByteArray(bd.getInputStream()));
          } catch (IOException e) {
            throw new SQLException(e);
          }
        }
        break;
      case CLOB:
        if (bd == null || bd.isNull()) { ps.setNull(position, Types.CLOB); }
        else { ps.setClob(position, bd.getReader()); }
        break;
      case BLOB:
        if (bd == null || bd.isNull()) { ps.setNull(position, Types.BLOB);
        } else { ps.setBlob(position, bd.getInputStream()); }
        break;
      case OBJECT: ps.setObject(position, value);
    }
  }
}
