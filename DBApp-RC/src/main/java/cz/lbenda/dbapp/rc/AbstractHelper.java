/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.lbenda.dbapp.rc;

/** Class which help with some work.
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public abstract class AbstractHelper {

  /** This method run equals on two object. If both is null, then is equal */
  public static boolean nullEquals(Object o1, Object o2) {
    if (o1 == null) { return o2 == null; }
    return o1.equals(o2);
  }

}
