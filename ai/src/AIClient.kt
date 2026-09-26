package klite.ai

import klite.nodes.Node
import java.net.URI

interface AIClient {
  fun query(input: String, vararg imageUrl: URI, prevResponseId: String? = null, params: Node = emptyMap()): Response

  fun stream(input: String, vararg imageUrl: URI, params: Node = emptyMap()): Sequence<String>

  data class Response(val id: String?, val status: String, val model: String, val text: String)
}
