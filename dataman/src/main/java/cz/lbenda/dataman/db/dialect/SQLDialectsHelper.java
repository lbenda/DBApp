package cz.lbenda.dataman.db.dialect;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and beforeOpenInit the template in the editor.
 */
/**
 *
 * @author Lukas Benda <lbenda at lbenda.cz>
 */
public class SQLDialectsHelper {

  private static final SQLDialect DEFAULT = new H2Dialect();

  public static SQLDialect dialectForDriver(String driver) {
    if (SQLDialect.DIALECTS.isEmpty()) {
      SQLDialect.DIALECTS.add(new H2Dialect());
      SQLDialect.DIALECTS.add(new HSQLDBDialect());
    }
    for (SQLDialect dialect : SQLDialect.DIALECTS) {
      if (dialect.isForDriver(driver)) { return dialect; }
    }
    return DEFAULT;
  }
}
