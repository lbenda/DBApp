<?xml version="1.0"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ex="http://www.lbenda.cz/dataman/schema/Export">

  <xsl:template match="/ex:export">
    <html>
      <body>
        <h2>SQL Query: <xsl:value-of select="@sql" /></h2>
        <table border="1">
          <thead>
            <tr bgcolor="#9acd32">
              <xsl:for-each select="ex:columns/ex:column">
                <th><xsl:value-of select="@column" /></th>
              </xsl:for-each>
            </tr>
          </thead>
          <tbody>
            <xsl:for-each select="ex:row">
              <tr>
                <xsl:for-each select="ex:field">
                  <td><xsl:value-of select="text()"/></td>
                </xsl:for-each>
              </tr>
            </xsl:for-each>
          </tbody>
        </table>
      </body>
    </html>
  </xsl:template>

</xsl:stylesheet>