package klite.jdbc

import klite.Decimal
import klite.d
import klite.trimToNull
import klite.uuid
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.time.Instant
import java.time.Period
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.text.RegexOption.IGNORE_CASE
import kotlin.text.RegexOption.MULTILINE

@Suppress("UNCHECKED_CAST")
fun <T> ResultSet.get(column: String, type: KType): T = JdbcConverter.from(when (type.classifier) {
  Int::class -> getIntOrNull(column)
  Float::class -> getFloatOrNull(column)
  Decimal::class -> getDecimalOrNull(column)
  Instant::class -> getTimestamp(column)
  else -> getObjectUnwrapped(column)
}, type) as T

inline operator fun <reified T> ResultSet.get(column: String): T = get(column, typeOf<T>())

fun <T> ResultSet.getOptional(column: String, type: KType): Result<T> = runCatching { get(column, type) }
inline fun <reified T> ResultSet.getOptional(column: String): Result<T> = getOptional(column, typeOf<T>())

private val pgObjectGetValue: java.lang.reflect.Method? = runCatching {
  Class.forName("org.postgresql.util.PGobject").getMethod("getValue")
}.getOrNull()

fun ResultSet.getObjectUnwrapped(column: String): Any? {
  val o = getObject(column)
  return if (o != null && pgObjectGetValue != null && pgObjectGetValue.declaringClass.isInstance(o)) pgObjectGetValue.invoke(o)
         else o
}

fun ResultSet.getUuid(column: String = "id") = getString(column).uuid
fun ResultSet.getUuidOrNull(column: String = "id") = getString(column)?.uuid

fun ResultSet.getInstant(column: String) = getTimestamp(column).toInstant()
fun ResultSet.getInstantOrNull(column: String) = getTimestamp(column)?.toInstant()
fun ResultSet.getLocalDate(column: String) = getDate(column).toLocalDate()
fun ResultSet.getLocalDateOrNull(column: String) = getDate(column)?.toLocalDate()
fun ResultSet.getLocalDateTime(column: String) = getTimestamp(column).toLocalDateTime()
fun ResultSet.getLocalDateTimeOrNull(column: String) = getTimestamp(column)?.toLocalDateTime()
fun ResultSet.getLocalTime(column: String) = getTime(column).toLocalTime()
fun ResultSet.getLocalTimeOrNull(column: String) = getTime(column)?.toLocalTime()
fun ResultSet.getPeriod(column: String) = Period.parse(getString(column))
fun ResultSet.getPeriodOrNull(column: String) = getString(column)?.let { Period.parse(it) }
fun ResultSet.getDecimal(column: String) = getString(column).d

fun ResultSet.getIntOrNull(column: String) = getInt(column).takeUnless { wasNull() }
fun ResultSet.getLongOrNull(column: String) = getLong(column).takeUnless { wasNull() }
fun ResultSet.getFloatOrNull(column: String) = getFloat(column).takeUnless { wasNull() }
fun ResultSet.getDoubleOrNull(column: String) = getDouble(column).takeUnless { wasNull() }
fun ResultSet.getDecimalOrNull(column: String) = getString(column)?.d

inline fun <reified T: Enum<T>> ResultSet.getEnum(column: String) = enumValueOf<T>(getString(column))
inline fun <reified T: Enum<T>> ResultSet.getEnumOrNull(column: String) = getString(column)?.let { enumValueOf<T>(it) }

/**
 * Makes "alias.column" (joined table) columns accessible on any DB, e.g. getString("b.id").
 * Resolution is lazy — column metadata is only scanned when a dotted name is first used.
 */
internal fun ResultSet.withJoinPrefixes(select: String): ResultSet {
  val aliases = joinAliases(select)
  return if (aliases.isEmpty()) this
  else Proxy.newProxyInstance(ResultSet::class.java.classLoader, arrayOf(ResultSet::class.java),
    PrefixedColumns(this, aliases)) as ResultSet
}

private class PrefixedColumns(private val rs: ResultSet, private val aliases: List<String>): InvocationHandler {
  private val prefixes by lazy { joinedPrefixes(rs.metaData, aliases) }

  override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? = try {
    val label = args?.getOrNull(0) as? String
    if (label != null && '.' in label && method.parameterTypes[0] == String::class.java) {
      val index = prefixes[label] ?: label.lowercase().let { prefixes[it] ?: rs.findColumn(it) }
      if (method.name == "findColumn") index
      else ResultSet::class.java.getMethod(method.name, Integer.TYPE, *method.parameterTypes.drop(1).toTypedArray())
        .invoke(rs, index, *args.drop(1).toTypedArray())
    } else method.invoke(rs, *(args ?: emptyArray()))
  } catch (e: InvocationTargetException) {
    throw e.targetException
  }
}

private fun joinedPrefixes(md: ResultSetMetaData, aliases: List<String>): Map<String, Int> {
  val map = HashMap<String, Int>()
  var joinCount = 0
  var prevTable = ""
  var groupFirst = ""
  for (i in 1..md.columnCount) {
    val label = md.getColumnLabel(i).lowercase()
    val table = md.getTableName(i).lowercase()
    // new table on table name change; self-join repeats the same table name + first label
    if (joinCount == 0 || table.isNotEmpty() && table != prevTable) {
      joinCount++
      prevTable = table
      groupFirst = label
    } else if (label == groupFirst) joinCount++
    if (joinCount > 1) map.putIfAbsent("${aliases.getOrNull(joinCount - 2) ?: joinCount}.$label", i)
  }
  return map
}

private val joinRegex = "\\bjoin\\s+(\\w+?)(\\s+as)?(\\s+(\\w+?))?\\s+(on|using)\\b".toRegex(setOf(IGNORE_CASE, MULTILINE))
internal fun joinAliases(select: String) = joinRegex.findAll(select).map { it.groupValues[4].trimToNull() ?: it.groupValues[1] }.toList()
