package klite.jdbc

import ch.tutteli.atrium.api.fluent.en_GB.toContainExactly
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject
import java.sql.ResultSet
import java.sql.ResultSetMetaData

class ResultSetsTest {
  val rs = mockk<ResultSet>()

  @Test fun getObjectUnwrapped() {
    every { rs.getObject("x") } returns PGobject().apply { value = "citext" }
    expect(rs.getObjectUnwrapped("x")).toEqual("citext")
  }

  @Test fun joinAliases() {
    expect(joinAliases("select * from table1 join table2 on x=y left join table3 t3 on t3.x = y inner join table4 as t4 using(id) where ..."))
      .toContainExactly("table2", "t3", "t4")

    expect(joinAliases("select * from table1\njoin table2\n   on x=y\nleft join\ntable3 t3 on t3.x = y inner join table4 as t4 using(id) where ..."))
      .toContainExactly("table2", "t3", "t4")

    expect(joinAliases("select a, b from table1 t1 join table2 t2 using (field) where ... order by id"))
      .toContainExactly("t2")
  }

  @Test fun `joined column prefixes`() {
    val rs = mockk<ResultSet> {
      every { metaData } returns md("id" to "t1", "name" to "t1", "id" to "t2", "code" to "t2")
      every { getString("id") } returns "first"
      every { getString(3) } returns "second"
      every { getObject(4) } returns "code"
    }
    val wrapped = rs.withJoinPrefixes("select * from t1 join t2 on x=y")
    expect(wrapped.getString("t2.id")).toEqual("second")
    expect(wrapped.getObject("t2.code")).toEqual("code" as Any?)
    expect(wrapped.getString("id")).toEqual("first")
  }

  @Test fun `self-join without id`() {
    val rs = mockk<ResultSet> {
      every { metaData } returns md("x" to "t", "name" to "t", "x" to "t", "name" to "t")
      every { getString("x") } returns "a.x"
      every { getString(3) } returns "b.x"
      every { getString(4) } returns "b.name"
    }
    val wrapped = rs.withJoinPrefixes("select * from t a join t b on a.x = b.x")
    expect(wrapped.getString("b.x")).toEqual("b.x")
    expect(wrapped.getString("b.name")).toEqual("b.name")
    expect(wrapped.getString("x")).toEqual("a.x")
  }

  private fun md(vararg columns: Pair<String, String>) = mockk<ResultSetMetaData> {
    every { columnCount } returns columns.size
    columns.forEachIndexed { i, (label, table) ->
      every { getColumnLabel(i + 1) } returns label
      every { getTableName(i + 1) } returns table
    }
  }
}
