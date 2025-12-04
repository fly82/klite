package klite

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class OpenMetricsRendererTest {
  @Test fun render() {
    val out = ByteArrayOutputStream()
    OpenMetricsRenderer().render(out, mapOf("myNumber" to 42, "myMap" to mapOf("aValue" to 3.14), "myInfo" to "text"))
    expect(out.toString().replace("\r\n", "\n")).toEqual("""
      # TYPE my_number counter
      my_number 42
      # TYPE my_map_a_value gauge
      my_map_a_value 3.14
      # TYPE my_info info
      my_info{value="text"} 1
      # EOF
    """.trimIndent() + "\n")
  }
}
