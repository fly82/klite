package klite.http

import ch.tutteli.atrium.api.fluent.en_GB.messageToContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import com.sun.net.httpserver.HttpServer
import klite.StatusCode
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.http.HttpClient
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class TypedHttpClientTest {
  @Test fun `logger name should get container class name`() {
    expect(object: TypedHttpClient(http = HttpClient.newHttpClient(), contentType = "") {}.logger.name)
      .toEqual(javaClass.name)
  }

  @Test fun `request should not be retried on a client error`() {
    ErrorRespondingServer(StatusCode.BadRequest).use { server ->
      expect { server.client.get<String>("/") }.toThrow<HttpException>().messageToContain("Failed with 400")
      expect(server.requests.get()).toEqual(1)
    }
  }

  @Test fun `request should be retried on a server error`() {
    ErrorRespondingServer(StatusCode.ServiceUnavailable).use { server ->
      expect { server.client.get<String>("/") }.toThrow<HttpException>().messageToContain("Failed with 503")
      expect(server.requests.get()).toEqual(4)
    }
  }
}

private class ErrorRespondingServer(status: StatusCode, retryCount: Int = 3): AutoCloseable {
  private val http = HttpServer.create(InetSocketAddress(0), 0)
  val requests = AtomicInteger()
  val client = object: TypedHttpClient("http://localhost:${http.address.port}", retryCount = retryCount, retryAfter = 0.seconds,
    http = HttpClient.newHttpClient(), contentType = "text/plain") {}

  init {
    http.createContext("/") { exchange ->
      requests.incrementAndGet()
      val body = "error".toByteArray()
      exchange.sendResponseHeaders(status.value, body.size.toLong())
      exchange.responseBody.use { it.write(body) }
    }
    http.start()
  }

  override fun close() = http.stop(0)
}
