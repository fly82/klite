package klite.ai

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.every
import io.mockk.mockk
import klite.StatusCode
import klite.http.HttpException
import org.junit.jupiter.api.Test

class DataExtractorTest {
  val response = AIClient.Response("r1", "success", "test-model", """{"key": "value"}""")
  val aiClient = mockk<AIClient>(relaxed = true) {
    every { query(any()) } returns response
  }
  val extractor = DataExtractor(aiClient)

  @Test fun `extract strips markdown code fences`() {
    every { aiClient.query(any()) } returns response.copy(text = "```json\n{\"key\": \"value\"}\n```")

    expect(extractor.extract<TestClass>("test").key).toEqual("value")
  }

  @Test fun `extract retries on failure`() {
    var attempts = 0
    every { aiClient.query(any()) } answers {
      attempts++
      if (attempts < 3) throw RuntimeException("API error")
      response.copy(text = """{"key": "recovered"}""")
    }

    expect(extractor.extract<TestClass>("test").key).toEqual("recovered")
    expect(attempts).toEqual(3)
  }

  @Test fun `extract does not retry on a malformed request`() {
    var attempts = 0
    every { aiClient.query(any()) } answers {
      attempts++
      throw HttpException(StatusCode.BadRequest, "invalid_request_error")
    }

    expect { extractor.extract<TestClass>("test") }.toThrow<HttpException>()
    expect(attempts).toEqual(1)
  }

  @Test fun `extract does not retry on too many requests`() {
    var attempts = 0
    every { aiClient.query(any()) } answers {
      attempts++
      throw HttpException(StatusCode.TooManyRequests, "rate_limit_error")
    }

    expect { extractor.extract<TestClass>("test") }.toThrow<HttpException>()
    expect(attempts).toEqual(1)
  }

  data class TestClass(val key: String)
}
