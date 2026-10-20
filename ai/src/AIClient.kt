package klite.ai

import klite.Config
import klite.MimeTypes
import klite.ai.AIClient.TokenUsage
import klite.base64Encode
import klite.nodes.Node
import java.net.URI
import kotlin.time.Duration.Companion.seconds

interface AIClient {
  val timeout get() = Config.optional("AI_TIMEOUT_SEC", "30").toInt().seconds
  val maxLoggedLen get() = Config.optional("AI_LOG_LEN", "20000").toInt()

  fun query(input: String, vararg fileUrl: URI, prevResponseId: String? = null, params: Node = emptyMap()): Response

  fun stream(input: String, vararg fileUrl: URI, params: Node = emptyMap()): Sequence<String>

  data class Response(val id: String?, val status: String, val model: String, val text: String, val usage: TokenUsage? = null)
  data class TokenUsage(val model: String, val input: Int, val output: Int, val cached: Int = 0, val thought: Int = 0, val params: Node = emptyMap()) {
    operator fun plus(other: TokenUsage) = TokenUsage(model, input + other.input, output + other.output, cached + other.cached, thought + other.thought, params + other.params)
  }
}

operator fun TokenUsage?.plus(other: TokenUsage?): TokenUsage? = if (this == null) other else other?.let { this + it } ?: this

internal val URI.isImage get() = mimeType.orEmpty().startsWith("image/")

internal val URI.mimeType: String? get() = when (scheme) {
  "data" -> schemeSpecificPart.substringBefore(';').substringBefore(',').ifEmpty { null }
  else -> MimeTypes.typeFor(path ?: "")
}

internal fun URI.inlineBase64() = if (scheme == "data") schemeSpecificPart.substringAfter(',') else toURL().readBytes().base64Encode()
