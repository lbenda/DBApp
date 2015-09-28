package cz.lbenda.common;

import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Created by Lukas Benda <lbenda @ lbenda.cz> on 28.9.15. */
public class TestAbstractHelper {

  @DataProvider
  public static Object[][] binaryStrings() {
    return new Object[][] {
      new Object[] { "0000 0000" , new byte[] { 0 } },
      new Object[] { "0000 0001" , new byte[] { 1 } },
      new Object[] { "1010 0101" , new byte[] { -91 } },
      new Object[] { "0111 1111" , new byte[] { 127 } },
      new Object[] { "1000 0000" , new byte[] { -128 } },
      new Object[] { "1111 1111" , new byte[] { -1 } },
      new Object[] { "0111 1111 0011 1000" , new byte[] { 127, 56 } }
    };
  }

  @Test(dataProvider = "binaryStrings")
  public void printBitBinary(String binaryString, byte[] bytes) {
    assertEquals(AbstractHelper.printBitBinary(bytes), binaryString);
  }

  @Test(dataProvider = "binaryStrings")
  public void parseBitBinary(String binaryString, byte[] bytes) {
    assertEquals(AbstractHelper.parseBitBinary(binaryString), bytes);
  }
}