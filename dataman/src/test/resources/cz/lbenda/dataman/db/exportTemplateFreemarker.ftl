Export SQL query: ${sql}

<html>
<body>
  <table>
    <thead>
      <tr>
        <#list columns as column>
          <th>${column.name}</th>
        </#list>
      </tr>
    </thead>
    <tbody>
      <#list rows as row>
        <tr>
          <#list row as field>
            <td>
              ${field_index}
            </td>
          </#list>
        </tr>
      </#list>
    </tbody>
  </table>
</body>
</html>