package klite.i18n

import ch.tutteli.atrium.api.fluent.en_GB.toContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import klite.HttpExchange
import org.junit.jupiter.api.Test

@Suppress("UNCHECKED_CAST")
class LangTest {
  val exchange = mockk<HttpExchange>(relaxed = true) {
    every { cookie(Lang.COOKIE) } returns null
  }

  @Test fun `detect from cookie`() {
    every { exchange.cookie(Lang.COOKIE) } returns "en"
    expect(exchange.lang).toEqual("en")
  }

  @Test fun `detect from cookie not available`() {
    every { exchange.cookie(Lang.COOKIE) } returns "xy"
    expect(exchange.lang).toEqual("en")
  }

  @Test fun `invalid cookie falls back to Accept-Language`() {
    every { exchange.cookie(Lang.COOKIE) } returns "xy"
    every { exchange.header("Accept-Language") } returns "et"
    expect(exchange.lang).toEqual("et")
  }

  @Test fun `detect from Accept-Language`() {
    every { exchange.header("Accept-Language") } returns "et-EE,et;q=0.9,en;q=0.8"
    expect(exchange.lang).toEqual("et")
  }

  @Test fun `detect from Accept-Language without country`() {
    every { exchange.header("Accept-Language") } returns "et;q=0.9,en;q=0.8"
    expect(exchange.lang).toEqual("et")
  }

  @Test fun `detect from Accept-Language skips unavailable`() {
    every { exchange.header("Accept-Language") } returns "jp,en;q=0.9"
    expect(exchange.lang).toEqual("en")
  }

  @Test fun `cookie preferred over Accept-Language`() {
    every { exchange.cookie(Lang.COOKIE) } returns "en"
    every { exchange.header("Accept-Language") } returns "et"
    expect(exchange.lang).toEqual("en")
  }

  @Test fun `fallback to default`() {
    every { exchange.header("Accept-Language") } returns null
    expect(exchange.lang).toEqual("en")
  }

  @Test fun remember() {
    exchange.lang = "et"
    verify { exchange.cookie(Lang.COOKIE, "et") }
  }

  @Test fun readAvailableLangs() {
    expect(Lang.available).toContain("en")
  }

  @Test fun translations() {
    expect((Lang.translations("en")["greeting"] as Map<String, *>)["hello"] as String).toEqual("Hello")
  }

  @Test fun translate() {
    val translate = Lang.translations("en")
    expect(translate("title")).toEqual("Klite Test")
    expect(translate("greeting.hello")).toEqual("Hello")
    expect(translate("greeting.blah.notExists.yet")).toEqual("greeting.blah.notExists.yet")
  }

  @Test fun `translate with fallback`() {
    val translate = Lang.translations("et")
    expect(translate("title")).toEqual("Klite Test")
    expect(translate("greeting.hello")).toEqual("Tere")
  }

  @Test fun `translate with substitutions`() {
    expect(Lang.translate("en", "greeting.helloName", mapOf("name" to "World"))).toContain("Hello, World!")
  }
}
