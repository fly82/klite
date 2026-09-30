package klite.jdbc

import klite.Config
import klite.createFrom
import klite.info
import klite.logger
import java.io.FileNotFoundException
import java.io.Reader
import java.net.JarURLConnection
import java.net.URL
import kotlin.reflect.full.primaryConstructor

/**
 * Reads DB changesets from sql files, similar to [Liquibase SQL format](https://docs.liquibase.com/concepts/changelogs/sql-format.html).
 * Default implementation reads files from classpath.
 *
 * Lines starting with the following are treated specially:
 * * `--include path/file.sql` includes another sql file from classpath
 * * `--substitute a=b` can be used to substitute ${a} on subsequent lines with b, or use Config/env vars
 * * `--changeset some-id onChange:SKIP` - changeset declarations themselves, starting with its unique id and optional attributes
 */
open class ChangeSetFileReader(
  private vararg val filePaths: String,
  private val substitutions: MutableMap<String, String> = mutableMapOf(),
): Sequence<ChangeSet> {
  private val log = logger()
  private val keywords = setOf("include", "substitute", "changeset")
  private val changeSetParams = ChangeSet::class.primaryConstructor!!.parameters.map { it.name }.toSet()
  private val commentRegex = "\\s*--[^']*$".toRegex()
  private val substRegex = "(?<!\\\$)\\\$\\{(\\w*)}".toRegex()
  private val whitespace = "\\s+".toRegex()

  override fun iterator() = readAll().iterator()
  private fun readAll(): Sequence<ChangeSet> = sequence { filePaths.forEach { readFile(it) } }

  open fun resolve(path: String): URL = Thread.currentThread().contextClassLoader.getResource(path) ?: throw FileNotFoundException("$path not found in classpath")
  open fun resolveFile(path: String) = resolve(path).openStream()

  /** Jar entry CRC-32 if available without reading content, otherwise file lastModified */
  open fun fileCheck(path: String): Long = resolve(path).openConnection().let { c ->
    (c as? JarURLConnection)?.jarEntry?.crc?.takeIf { it >= 0L } ?: c.lastModified
  }

  /** Combined stamp of all files including `--include`d ones, without parsing changesets */
  fun combinedStamp(): Long {
    val files = sortedMapOf<String, Long>()
    fun walk(path: String) {
      if (path in files) return
      files[path] = fileCheck(path)
      resolve(path).openStream().bufferedReader().forEachLine { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("--include ")) walk(trimmed.substringAfter("--include ").trim())
      }
    }
    filePaths.forEach(::walk)
    return files.entries.fold(0L) { r, e -> r * 89 + e.key.hashCode() * 31 + e.value }
  }

  private suspend fun SequenceScope<ChangeSet>.readFile(path: String) = resolveFile(path).reader().use { read(it, path) }

  private suspend fun SequenceScope<ChangeSet>.read(reader: Reader, path: String) {
    log.info("Reading $path")
    var changeSet: ChangeSet? = null

    reader.buffered().lineSequence().map { it.trimEnd() }.filter { it.isNotEmpty() }.forEach { line ->
      if (line.startsWith("--")) {
        val parts = line.substring(2).trim().split(whitespace)
        if (parts[0] in keywords && changeSet != null) {
          yield(changeSet.finish())
          changeSet = null
        }
        when (parts[0]) {
          "include" -> readFile(parts[1])
          "substitute" -> {
            val (key, value) = parts.drop(1).joinToString(" ").split(':', '=')
            substitutions[key] = value
          }
          "changeset" -> {
            changeSet = ChangeSet::class.createFrom(mapOf(ChangeSet::id.name to parts[1], ChangeSet::filePath.name to path) +
              parts.drop(2).associate { it.split(':').let { (k, v) ->
                require(k in changeSetParams) { "Unknown changeset param $k, supported: $changeSetParams" }
                k to v
              }})
          }
        }
      }
      else if (changeSet == null) error("No --changeset declaration preceding: $line")
      else changeSet.addLine(line.replace(commentRegex, "").substitute())
    }
    if (changeSet != null) yield(changeSet.finish())
  }

  private fun String.substitute() = substRegex.replace(this) {
    val key = it.groupValues[1]
    substitutions[key] ?: (Config.optional(key) ?: error("Unknown substitution ${it.value}"))
      .apply { require(!contains(';')) { "Multiple statements not allowed in substitution $key" } }
  }
}
