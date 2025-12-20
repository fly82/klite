package klite.jdbc

import klite.Config
import klite.info
import klite.logger
import java.io.File
import java.io.IOException
import java.lang.ProcessBuilder.Redirect.INHERIT
import kotlin.system.measureTimeMillis

object DockerCompose {
  private val log = logger()
  private var compose = Config.optional("DOCKER_COMPOSE", "docker compose")

  fun run(command: String): Int = try {
    val fullCommand = "$compose $command"
    log.info("$fullCommand in ${File(".").absolutePath}")
    ProcessBuilder(fullCommand.split(' ')).redirectErrorStream(true).redirectOutput(INHERIT).start().waitFor()
  } catch (e: Exception) {
    if (Config.optional("DOCKER_COMPOSE") == null) {
      compose = "docker-compose"
      run(command)
    } else throw e
  }

  fun up(service: String, wait: Boolean = true) {
    if (run("up -d${if (wait) " --wait" else ""} $service") != 0) throw IOException("Failed to start $service")
  }

  fun startDB(service: String = Config.optional("DB_START", "db"), composeWait: Boolean = false) {
    if (service.isEmpty()) return
    val ms = measureTimeMillis { up(service, composeWait) }
    if (ms > 200) ConfigDataSource().waitForAcceptConnections()
  }
}

@Deprecated("Use DockerCompose.startDB instead", replaceWith = ReplaceWith("DockerCompose.startDB(service)"))
fun startDevDB(service: String = Config.optional("DB_START", "db")) = DockerCompose.startDB(service)
