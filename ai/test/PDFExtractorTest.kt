package klite.ai

import ch.tutteli.atrium.api.fluent.en_GB.toContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.base64Encode
import klite.nodes.Node
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.net.URI

@Suppress("DEPRECATION")
class PDFExtractorTest {
  @Test fun `whole pdf content is sent to the ai client as a data url`() {
    val pdf = byteArrayOf(1, 2, 3)
    val aiClient = RecordingAIClient()

    val invoice = PDFExtractor(DataExtractor(aiClient))
      .extractData<Invoice>(ByteArrayInputStream(pdf), extraPrompt = "The currency is always EUR")

    expect(invoice.number).toEqual("123")
    expect(aiClient.files.single()).toEqual(URI("data:application/pdf;base64,${pdf.base64Encode()}"))
    expect(aiClient.input).toContain("The currency is always EUR")
  }

  data class Invoice(val number: String)
}

private class RecordingAIClient(private val text: String = """{"number": "123"}""") : AIClient {
  lateinit var input: String
  var files: List<URI> = emptyList()

  override fun query(input: String, vararg fileUrl: URI, prevResponseId: String?, params: Node): AIClient.Response {
    this.input = input
    files = fileUrl.toList()
    return AIClient.Response("r1", "success", "test-model", text)
  }

  override fun stream(input: String, vararg fileUrl: URI, params: Node): Sequence<String> = emptySequence()
}
