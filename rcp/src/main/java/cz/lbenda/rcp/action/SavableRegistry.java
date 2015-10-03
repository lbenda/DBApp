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
package cz.lbenda.rcp.action;

import cz.lbenda.rcp.DialogHelper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 22.9.15. */
public class SavableRegistry {

  private static final SavableRegistry instance = new SavableRegistry(null);

  /** Return global savable register for whole application */
  public static @Nonnull SavableRegistry getInstance() { return instance; }
  /** Return new instance of savable register which is hold by global savable. */
  @SuppressWarnings("unused")
  public static @Nonnull SavableRegistry newInstance() { return newInstance(getInstance()); }

  /** Return new instance of savable register which is hold by parent
   * @param parent parent which hold this registry */
  public static SavableRegistry newInstance(SavableRegistry parent) {
    SavableRegistry result = new SavableRegistry(parent);
    synchronized (parent.children) { parent.children.add(result); }
    return result;
  }

  private final Set<Savable> registry = Collections.newSetFromMap(new WeakHashMap<>());
  private final Set<SavableRegistry> children = Collections.newSetFromMap(new WeakHashMap<>());
  private final SavableRegistry parent;
  private BooleanProperty dirtyProperty = new SimpleBooleanProperty(false);

  public SavableRegistry(SavableRegistry parent) {
    this.parent = parent;
    if (parent != null) {
      dirtyProperty.addListener((observer, oldValue, newValue) -> {
        if (newValue) { parent.dirtyProperty().setValue(true); }
        else { parent.recalculateDirty(); }
      });
    }
  }

  @SuppressWarnings("unused")
  public BooleanProperty dirtyProperty() { return dirtyProperty; }

  private void recalculateDirty() {
    boolean dirty;
    synchronized (registry) {
      dirty = registry.stream().anyMatch(sav -> Boolean.TRUE.equals(sav.dirtyProperty().getValue()));
    }
    if (!dirty) {
      synchronized (children) {
        dirty = children.stream().anyMatch(rs -> Boolean.TRUE.equals(rs.dirtyProperty().getValue()));
      }
    }
    dirtyProperty.setValue(dirty);
  }

  /** Close with open dialog
   * @return true when registry was closed */
  public boolean close() {
    boolean result = DialogHelper.getInstance().showUnsavedObjectDialog(this);
    if (result) { closeWithoutAsking(); }
    return result;
  }

  /** Close register without asking */
  @SuppressWarnings("unused")
  public void closeWithoutAsking() {
    if (parent != null) { synchronized (parent.children) { parent.children.remove(this); } }
    synchronized (registry) { registry.clear(); }
  }

  /** Dirty property listener which inform about dirty registry */
  private final ChangeListener<Boolean> dirtyPropertyListener = (observable, oldValue, newValue) -> {
    if (newValue) { dirtyProperty.setValue(true); }
    else { recalculateDirty(); }
  };

  /** Register savable entity */
  public void register(Savable savable) {
    synchronized (registry) {
      registry.add(savable);
    }
    savable.dirtyProperty().addListener(dirtyPropertyListener);
  }
  /** Register savable entity */
  public void unregister(Savable savable) {
    synchronized (registry) {
      registry.add(savable);
    }
    savable.dirtyProperty().removeListener(dirtyPropertyListener);
  }

  /** Return list of dirty savable */
  public Set<Savable> dirtySavables() {
    Set<Savable> result;
    synchronized (registry) {
      result = registry.stream().filter(savable -> Boolean.TRUE.equals(savable.dirtyProperty().getValue()))
          .collect(Collectors.toSet());
    }
    synchronized (children) {
      children.stream().filter(sr -> Boolean.TRUE.equals(sr.dirtyProperty().getValue()))
          .map(SavableRegistry::dirtySavables).forEach(result::addAll);
    }
    return result;
  }
}
