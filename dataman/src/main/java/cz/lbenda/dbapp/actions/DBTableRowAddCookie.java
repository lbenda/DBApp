/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dbapp.actions;

import org.openide.nodes.Node;

/** Interface which implements application which adding row to table
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public interface DBTableRowAddCookie extends Node.Cookie {
  void addRow();
}
