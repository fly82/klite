package klite.http

import klite.StatusCode
import klite.error
import klite.info
import klite.logger
import java.lang.System.currentTimeMillis
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublisher
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler
import java.net.http.HttpResponse.BodyHandlers.ofInputStream
import java.net.http.HttpResponse.BodyHandlers.ofString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

inline fun httpClient(builder: HttpClient.Builder.() -> Unit = {}): HttpClient =
  HttpClient.newBuilder().connectTimeout(5.seconds).apply(builder).build()

fun HttpClient.Builder.connectTimeout(duration: Duration): HttpClient.Builder = connectTimeout(duration.toJavaDuration())

fun HttpRequest.Builder.timeout(duration: Duration): HttpRequest.Builder = timeout(duration.toJavaDuration())
fun HttpRequest.Builder.authBearer(token: String) = setHeader("Authorization", "Bearer $token")
fun HttpRequest.Builder.contentType(mimeType: String) = setHeader("Content-Type", mimeType)
fun HttpRequest.Builder.accept(mimeType: String) = setHeader("Accept", mimeType)

typealias RequestModifier = HttpRequest.Builder.() -> Unit

private val log = logger<HttpClient>()

fun <R> HttpClient.request(url: URI, bodyHandler: BodyHandler<R>, modifier: RequestModifier = {}): HttpResponse<R> {
  val start = currentTimeMillis()
  val req = HttpRequest.newBuilder().uri(url).timeout(1.minutes).apply(modifier).build()
  try {
    val res = send(req, bodyHandler)
    log.info("${req.method()} $url in ${currentTimeMillis() - start}ms - ${res.statusCode()}")
    return res
  } catch (e: Exception) {
    log.error("${req.method()} $url in ${currentTimeMillis() - start}ms - failed: ${e.message}")
    throw e
  }
}

fun HttpClient.get(url: URI, modifier: RequestModifier = {}) = request(url, ofString()) { GET().apply(modifier) }
fun HttpClient.post(url: URI, data: Any?, modifier: RequestModifier = {}) = request(url, ofString()) { POST(toBodyPublisher(data)).apply(modifier) }
fun HttpClient.put(url: URI, data: Any?, modifier: RequestModifier = {}) = request(url, ofString()) { PUT(toBodyPublisher(data)).apply(modifier) }
fun HttpClient.patch(url: URI, data: Any?, modifier: RequestModifier = {}) = request(url, ofString()) { method("PATCH", toBodyPublisher(data)).apply(modifier) }
fun HttpClient.delete(url: URI, modifier: RequestModifier = {}) = request(url, ofString()) { DELETE().apply(modifier) }

fun HttpClient.getStreaming(url: URI, modifier: RequestModifier = {}) = request(url, ofInputStream()) { GET().apply(modifier) }
fun HttpClient.postStreaming(url: URI, data: Any?, modifier: RequestModifier = {}) = request(url, ofInputStream()) { POST(toBodyPublisher(data)).apply(modifier) }

fun toBodyPublisher(data: Any?): BodyPublisher = when (data) {
  null, Unit -> BodyPublishers.noBody()
  is BodyPublisher -> data
  is ByteArray -> BodyPublishers.ofByteArray(data)
  else -> BodyPublishers.ofString(data.toString())
}

fun <T> HttpResponse<T>.bodyOrThrow(): T {
  if (statusCode() >= 300) throw HttpException(StatusCode(statusCode()), body().toString())
  return body()
}
