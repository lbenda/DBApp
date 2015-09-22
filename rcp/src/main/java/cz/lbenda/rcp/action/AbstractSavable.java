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

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 11.9.15.
 * Implementation of savable interface */
@SuppressWarnings("JavaDoc")
public abstract class AbstractSavable implements Savable {

  /** Savable register for current savable object. The default is global savable register. */
  public SavableRegistry savableRegistry = SavableRegistry.getInstance();

  /** Set savable register for object */
  public void setSavableRegister(SavableRegistry savableRegister) {
    this.savableRegistry = savableRegister;
  }

  /** Tells the system to register this {@link Savable} into {@link SavableRegistry#registry}.
   * Only one {@link Savable} (according to {@link #equals(java.lang.Object)} and
   * {@link #hashCode()}) can be in the registry. New call to {@link AbstractSavable#register()}
   * replaces any previously registered and equal {@link Savable}s. After this call
   * the {@link SavableRegistry#registry} holds a strong reference to <code>this</code>
   * which prevents <code>this</code> object to be garbage collected until it
   * is {@link #unregister() unregistered} or {@link #register() replaced by
   * equal one}.
   */
  protected final void register() {
    savableRegistry.register(this);
  }

  /** Removes this {@link Savable} from the {@link SavableRegistry#registry} (if it
   * is present there, by relying on {@link #equals(java.lang.Object)}
   * and {@link #hashCode()}).
   */
  protected final void unregister() {
    savableRegistry.unregister(this);
  }

  /** Equals and {@link #hashCode} need to be properly implemented
   * by subclasses to correctly implement equality contract.
   * Two {@link Savable}s should be equal
   * if they represent the same underlying object beneath them. Without
   * correct implementation proper behavior of {@link #register()} and
   * {@link #unregister()} cannot be guaranteed.
   *
   * @param obj object to compare this one to,
   * @return true or false
   */
  @Override
  public abstract boolean equals(Object obj);

  /** HashCode and {@link #equals} need to be properly implemented
   * by subclasses, so two {@link Savable}s representing the same object
   * beneath are really equal and have the same {@link Object#hashCode()}.
   * @return integer hash
   * @see #equals(java.lang.Object)
   */
  @Override
  public abstract int hashCode();
}
