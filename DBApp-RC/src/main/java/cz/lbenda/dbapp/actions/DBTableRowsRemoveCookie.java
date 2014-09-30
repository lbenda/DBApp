/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.actions;

import org.openide.nodes.Node;

/** Cookie for remove table rows
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public interface DBTableRowsRemoveCookie extends Node.Cookie {
  void removeRows();
}
