package klite.ai

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import klite.SnakeCase
import klite.base64Encode
import klite.json.JsonMapper
import klite.json.parse
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI
import java.time.Instant

class OpenAIClientTest {
  @Test fun `usage is parsed from response json`() {
    val json = """{"input_tokens":1152,"input_tokens_details":{"cache_write_tokens":1149,"cached_tokens":0},
      "output_tokens":251,"output_tokens_details":{"reasoning_tokens":146},"total_tokens":1403}"""

    expect(JsonMapper(keys = SnakeCase).parse<AIClient.Usage>(json))
      .toEqual(AIClient.Usage(inputTokens = 1152, outputTokens = 251))
  }

  @Test fun `usage is passed to the text response`() {
    val output = OpenAIClient.Output("o1", "message", listOf(OpenAIClient.Content("output_text", "hi")))
    val response = OpenAIClient.Response("r1", Instant.now(), "completed", "gpt-5", listOf(output), AIClient.Usage(1152, 251))

    expect(response.toTextResponse()).toEqual(AIClient.Response("r1", "completed", "gpt-5", "hi", AIClient.Usage(1152, 251, "gpt-5")))
  }

  @Test fun `text response has no usage when it is absent`() {
    val output = OpenAIClient.Output("o1", "message", listOf(OpenAIClient.Content("output_text", "hi")))
    val response = OpenAIClient.Response("r1", Instant.now(), "completed", "gpt-5", listOf(output))

    expect(response.toTextResponse().usage).toEqual(null)
  }

  @Test fun `remote document is referenced by url`() {
    val url = URI("https://files.pixit.vet/p1/visit/report.pdf")

    // OpenAI rejects filename together with file_url as mutually exclusive parameters
    expect(url.toOpenAIContent()).toEqual(OpenAIClient.Content(type = "input_file", fileUrl = url))
  }

  @Test fun `remote image is referenced by url`() {
    val url = URI("https://files.pixit.vet/p1/vaccination/scan.jpg")

    expect(url.toOpenAIContent()).toEqual(OpenAIClient.Content(type = "input_image", imageUrl = url))
  }

  @Test fun `local document is inlined as base64 data`() {
    val file = File.createTempFile("report", ".pdf").apply { writeBytes(byteArrayOf(1, 2, 3)) }

    val content = file.toURI().toOpenAIContent()

    expect(content).toEqual(OpenAIClient.Content(type = "input_file", fileData = URI("data:application/pdf;base64,${file.readBytes().base64Encode()}"), filename = file.name))
  }

  @Test fun `local image is inlined as base64 data`() {
    val file = File.createTempFile("scan", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }

    val content = file.toURI().toOpenAIContent()

    expect(content).toEqual(OpenAIClient.Content(type = "input_image", imageUrl = URI("data:image/jpeg;base64,${file.readBytes().base64Encode()}")))
  }

  @Test fun `data url document is inlined as is`() {
    val data = URI("data:application/pdf;base64,AAAA")
    expect(data.toOpenAIContent()).toEqual(OpenAIClient.Content(type = "input_file", fileData = data, filename = "file.pdf"))
  }
}
