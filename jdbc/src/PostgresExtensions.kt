package klite.jdbc

import klite.logger
import klite.warn
import java.sql.Types

fun DB.lock(on: String) { call("pg_advisory_lock", lockKey(on)) }
fun DB.tryLock(on: String): Boolean = call("pg_try_advisory_lock", lockKey(on), returnSqlType = Types.BOOLEAN) == true
fun DB.unlock(on: String): Boolean = (call("pg_advisory_unlock", lockKey(on), returnSqlType = Types.BOOLEAN) == true).also {
  if (!it) logger().warn("Unlocking of $on failed in $this")
}

private fun lockKey(on: String): Long = on.fold(0L) { acc, char -> 31L * acc + char.code }
