package klite

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream

class SizeLimitedInputStreamTest {
  private fun stream(data: String, limit: Long) = SizeLimitedInputStream(data.byteInputStream(), limit)

  @Test fun `read within limit`() {
    val s = stream("Hello", 10)
    expect(s.readBytes().decodeToString()).toEqual("Hello")
  }

  @Test fun `read exactly at limit`() {
    val s = stream("Hello", 5)
    expect(s.readBytes().decodeToString()).toEqual("Hello")
  }

  @Test fun `read exceeds limit on bulk read`() {
    val s = stream("Hello World", 5)
    assertThrows<StatusCodeException> { s.readBytes() }
  }

  @Test fun `single-byte read exceeds limit`() {
    val s = stream("AB", 1)
    expect(s.read()).toEqual('A'.code)
    assertThrows<StatusCodeException> { s.read() }
  }

  @Test fun `skip exceeds limit`() {
    val s = stream("Hello", 3)
    assertThrows<StatusCodeException> { s.skip(5) }
  }

  @Test fun `available reflects remaining allowance`() {
    val s = stream("Hello World", 10)
    expect(s.available()).toEqual(10)
    s.read() // 1 byte
    expect(s.available()).toEqual(9)
  }

  @Test fun `available capped by underlying stream`() {
    val s = stream("Hi", 100)
    expect(s.available()).toEqual(2)
  }

  @Test fun `EOF does not throw`() {
    val s = stream("", 10)
    expect(s.read()).toEqual(-1)
  }

  @Test fun `close delegates to source`() {
    var closed = false
    val src = object : ByteArrayInputStream(byteArrayOf()) {
      override fun close() { closed = true; super.close() }
    }
    SizeLimitedInputStream(src, 10).close()
    expect(closed).toEqual(true)
  }

  @Test fun `partial read then EOF within limit`() {
    val s = stream("Hi", 100)
    val buf = ByteArray(10)
    expect(s.read(buf, 0, 10)).toEqual(2)
    expect(s.read(buf, 0, 10)).toEqual(-1)
  }
}
