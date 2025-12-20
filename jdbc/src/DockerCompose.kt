package klite.jdbc

import klite.Config
import klite.info
import java.io.File
import java.io.IOException
import java.lang.ProcessBuilder.Redirect.INHERIT
import java.lang.System.getLogger
import java.lang.Thread.sleep
import java.sql.SQLException
import kotlin.system.measureTimeMillis

private val log = getLogger("dockerCompose")

fun dockerCompose(command: String): Int = try {
  val fullCommand = Config.optional("DOCKER_COMPOSE", "docker compose") + " " + command
  log.info("$fullCommand in ${File(".").absolutePath}")
  ProcessBuilder(fullCommand.split(' ')).redirectErrorStream(true).redirectOutput(INHERIT).start().waitFor()
} catch (e: Exception) {
  if (Config.optional("DOCKER_COMPOSE") == null) {
    Config["DOCKER_COMPOSE"] = "docker-compose"
    dockerCompose(command)
  } else throw e
}

fun dockerComposeUp(service: String, wait: Boolean = true) {
  if (dockerCompose("up -d${if (wait) " --wait" else ""} $service") != 0) throw IOException("Failed to start $service")
}

fun startDevDB(service: String = Config.optional("DB_START", "db"), composeWait: Boolean = false) {
  if (service.isEmpty()) return
  val ms = measureTimeMillis { dockerComposeUp(service, composeWait) }
  if (ms > 200) waitForDBToAcceptConnections()
}

private fun waitForDBToAcceptConnections(numTries: Int = 10, timeoutMs: Long = 500L) {
  val db = ConfigDataSource()
  var tries = 1
  do {
    try { db.connection.use { return } }
    catch (e: SQLException) {
      log.info("Retrying $tries: $e")
      sleep(timeoutMs)
    }
  } while (tries++ < numTries)
}
