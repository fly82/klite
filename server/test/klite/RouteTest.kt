package klite

import ch.tutteli.atrium.api.fluent.en_GB.notToEqualNull
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findAnnotation

annotation class Public

class RouteTest {
  @Test fun annotations() {
    val route = Route(RequestMethod.GET, "".toRegex()) {}
    expect(route.findAnnotation<Public>()).toEqual(null)

    val annotated = Route(RequestMethod.GET, "".toRegex(), listOf(Public())) {}
    expect(annotated.findAnnotation<Public>()).notToEqualNull()
  }

  @Test fun pathParams() {
    val route = Route(RequestMethod.GET, PathParamRegexer().from("/hello/:name/:id")) {}
    val match = route.path.matchEntire("/hello/world/42")!!
    val params = PathParams(match.groups, route.namedGroups)
    expect(params["name"]).toEqual("world")
    expect(params["id"]).toEqual("42")
    expect(params["missing"]).toEqual(null)
    expect(params.containsKey("missing")).toEqual(false)
  }
}
