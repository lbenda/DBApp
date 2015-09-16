/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.lbenda.dataman.db.dialect;

/**
 *
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public class H2Dialect extends HSQLDBDialect {

  static {
    SQLDialect.DIALECTS.add(new H2Dialect());
  }

  @Override
  public boolean isForDriver(String driver) {
    if (driver == null) { return false; }
    return driver.startsWith("org.h2");
  }

  @Override
  public String columnGenerated() {
    return null;
  }
}
