package klite.xml

internal class XmlElement(
  @JvmField val name: String,
  @JvmField val attributes: Map<String, String>? = null,
  @JvmField var text: String? = null,
  @JvmField var children: MutableList<XmlElement> = mutableListOf()
) {
  fun find(path: List<String>): List<XmlElement> {
    var e: XmlElement = this
    val path = if (path.first().substringBefore('[') == e.name) path.drop(1) else path
    for ((i, n) in path.withIndex()) {
      if (n == "" || n == ".") break
      if (n.startsWith("@")) return listOf(XmlElement(n, text = e.attributes?.get(n)))
      val elemName = n.substringBefore('[')
      val attr = n.substringAfter("[@", "").substringBefore(']').split('=', limit = 2)
      fun matches(c: XmlElement) = c.name == elemName && (attr.size < 2 || c.attributes?.get("@${attr[0]}") == attr[1])
      if (i == path.lastIndex) return e.children.filter(::matches)
      e = e.children.firstOrNull(::matches) ?: return emptyList()
    }
    return listOf(e)
  }
}
